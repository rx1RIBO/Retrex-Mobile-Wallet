package rext.org.rextwallet.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.rextj.core.Block;
import org.rextj.core.Coin;
import org.rextj.core.FilteredBlock;
import org.rextj.core.InsufficientMoneyException;
import org.rextj.core.Peer;
import org.rextj.core.Transaction;
import org.rextj.core.TransactionConfidence;
import org.rextj.core.listeners.AbstractPeerDataEventListener;
import org.rextj.core.listeners.PeerConnectedEventListener;
import org.rextj.core.listeners.PeerDataEventListener;
import org.rextj.core.listeners.PeerDisconnectedEventListener;
import org.rextj.core.listeners.TransactionConfidenceEventListener;
import org.rextj.wallet.SendRequest;
import org.rextj.wallet.Wallet;
import org.rextj.wallet.exceptions.RequestFailedErrorcodeException;
import org.rextj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import chain.BlockchainManager;
import chain.BlockchainState;
import chain.Impediment;
import global.wrappers.TransactionWrapper;
import host.furszy.zerocoinj.wallet.CannotSpendCoinsException;
import pivtrum.listeners.AddressListener;
import rext.org.rextwallet.RextApplication;
import rext.org.rextwallet.R;
import rext.org.rextwallet.module.RextContext;
import global.RextModuleImp;
import rext.org.rextwallet.module.store.AccStoreDb;
import rext.org.rextwallet.module.store.SnappyBlockchainStore;
import rext.org.rextwallet.module.store.StoredAccumulator;
import rext.org.rextwallet.rate.CoinMarketCapApiClient;
import rext.org.rextwallet.rate.RequestRextRateException;
import global.RextRate;
import rext.org.rextwallet.ui.wallet_activity.WalletActivity;
import rext.org.rextwallet.utils.AppConf;
import rext.org.rextwallet.utils.CrashReporter;

import static rext.org.rextwallet.module.RextContext.CONTEXT;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_ADDRESS_BALANCE_CHANGE;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_BROADCAST_TRANSACTION;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_CANCEL_COINS_RECEIVED;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_NOTIFICATION;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_RESET_BLOCKCHAIN;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_RESET_BLOCKCHAIN_ROLLBACK_TO;
import static rext.org.rextwallet.service.IntentsConstants.ACTION_SCHEDULE_SERVICE;
import static rext.org.rextwallet.service.IntentsConstants.DATA_TRANSACTION_HASH;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_BROADCAST_DATA_BLOCKCHAIN_STATE;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_BROADCAST_DATA_ON_COIN_RECEIVED;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_BROADCAST_DATA_PEER_CONNECTED;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_BROADCAST_DATA_TYPE;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_EXTRA_BLOCKCHAIN_STATE;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_TX_FAIL;
import static rext.org.rextwallet.service.IntentsConstants.INTENT_TX_SENT;
import static rext.org.rextwallet.service.IntentsConstants.NOT_BLOCKCHAIN_ALERT;
import static rext.org.rextwallet.service.IntentsConstants.NOT_COINS_RECEIVED;
import static rext.org.rextwallet.service.IntentsConstants.NOT_SPENDING_PROCESS;
import static rext.org.rextwallet.service.IntentsConstants.NOT_ZPIV_SEND_FAILED;
import static rext.org.rextwallet.service.IntentsConstants.NOT_ZPIV_SENT_COMPLETED;

/**
 * Created by furszy on 6/12/17.
 */

public class RextWalletService extends Service{

    private static final String CHANNEL_ID = "rext";
    private Logger log = LoggerFactory.getLogger(RextWalletService.class);

    private RextApplication rextApplication;
    private RextModuleImp module;
    private BlockchainManager blockchainManager;

    private PeerConnectivityListener peerConnectivityListener;

    private PowerManager.WakeLock wakeLock;
    private NotificationManager nm;
    private LocalBroadcastManager broadcastManager;

    private SnappyBlockchainStore blockchainStore;
    private AtomicBoolean resetBlockchainOnShutdown = new AtomicBoolean(false);
    private int resetToHeight = -1;
    /** Created service time (just for checks) */
    private long serviceCreatedAt;
    /** Cached amount to notify balance */
    private Coin notificationAccumulatedAmount = Coin.ZERO;
    /**  */
    private final Set<Impediment> impediments = EnumSet.noneOf(Impediment.class);

    private BlockchainState blockchainState = BlockchainState.NOT_CONNECTION;

    private volatile long lastUpdateTime = System.currentTimeMillis();
    private volatile long lastMessageTime = System.currentTimeMillis();

    private ExecutorService executor;

    private AtomicBoolean isSending = new AtomicBoolean(false);

    public class RextBinder extends Binder {
        public RextWalletService getService() {
            return RextWalletService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new RextBinder();
    }

    private AddressListener addressListener = new AddressListener() {
        @Override
        public void onBalanceChange(String address, long confirmed, long unconfirmed,int numConfirmations) {
            Intent intent = new Intent(ACTION_ADDRESS_BALANCE_CHANGE);
            broadcastManager.sendBroadcast(intent);
        }
    };

    private final class PeerConnectivityListener implements PeerConnectedEventListener, PeerDisconnectedEventListener{

        @Override
        public void onPeerConnected(Peer peer, int i) {
            //todo: notify peer connected
            log.info("Peer connected: "+peer.getAddress());
            broadcastPeerConnected();

            try {
                // Check if we have tx that have not been confirmed to send.
                for (Transaction transaction : module.listPendingTxes()) {
                    log.info("Trying to send not confirmed tx again.. --> " + transaction.getHashAsString());
                    peer.sendMessage(transaction);
                }
            }catch (Exception e){
                log.info("Exception on pending txes broadcast" ,e);
            }
        }

        @Override
        public void onPeerDisconnected(Peer peer, int i) {
            //todo: notify peer disconnected
            log.info("Peer disconnected: "+peer.getAddress());
            nm.cancelAll();
        }
    }

    private final PeerDataEventListener blockchainDownloadListener = new AbstractPeerDataEventListener() {

        @Override
        public void onBlocksDownloaded(final Peer peer, final Block block, final FilteredBlock filteredBlock, final int blocksLeft) {
            try {
                //log.info("Block received , left: " + blocksLeft);

            /*log.info("############# on Blockcs downloaded ###########");
            log.info("Peer: " + peer + ", Block: " + block + ", left: " + blocksLeft);*/


            /*if (RextContext.IS_TEST)
                showBlockchainSyncNotification(blocksLeft);*/

                //delayHandler.removeCallbacksAndMessages(null);


                final long now = System.currentTimeMillis();
                if (now - lastMessageTime > TimeUnit.SECONDS.toMillis(6)) {
                    if (blocksLeft < 6) {
                        blockchainState = BlockchainState.SYNC;
                    } else {
                        blockchainState = BlockchainState.SYNCING;
                    }
                    rextApplication.getAppConf().setLastBestChainBlockTime(block.getTime().getTime());
                    rextApplication.getWalletConfiguration().maybeIncrementBestChainHeightEver(module.getChainHeight());
                    broadcastBlockchainState(true);
                }
            }catch (Exception e){
                e.printStackTrace();
                CrashReporter.saveBackgroundTrace(e,rextApplication.getPackageInfo());
            }
        }
    };

    private class RunnableBlockChecker implements Runnable{

        private Block block;

        public RunnableBlockChecker(Block block) {
            this.block = block;
        }

        @Override
        public void run() {
            org.rextj.core.Context.propagate(RextContext.CONTEXT);
            lastMessageTime = System.currentTimeMillis();
            broadcastBlockchainState(false);
        }
    }

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                    final boolean hasConnectivity = networkInfo.isConnected();
                    log.info("network is {}, state {}/{}", hasConnectivity ? "up" : "down", networkInfo.getState(), networkInfo.getDetailedState());
                    if (hasConnectivity)
                        impediments.remove(Impediment.NETWORK);
                    else
                        impediments.add(Impediment.NETWORK);
                    check();
                    // try to request coin rate
                    requestRateCoin();
                } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                    log.info("device storage low");

                    impediments.add(Impediment.STORAGE);
                    check();
                } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                    log.info("device storage ok");
                    impediments.remove(Impediment.STORAGE);
                    check();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private WalletCoinsReceivedEventListener coinReceiverListener = new WalletCoinsReceivedEventListener() {

        android.support.v4.app.NotificationCompat.Builder mBuilder;
        PendingIntent deleteIntent;
        PendingIntent openPendingIntent;

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction transaction, Coin coin, Coin coin1) {
            //todo: acá falta una validación para saber si la transaccion es mia.
            org.rextj.core.Context.propagate(CONTEXT);

            try {
                int depthInBlocks = transaction.getConfidence().getDepthInBlocks();

                long now = System.currentTimeMillis();
                if (lastUpdateTime + 5000 < now) {
                    lastUpdateTime = now;
                    Intent intent = new Intent(ACTION_NOTIFICATION);
                    intent.putExtra(INTENT_BROADCAST_DATA_TYPE, INTENT_BROADCAST_DATA_ON_COIN_RECEIVED);
                    broadcastManager.sendBroadcast(intent);
                }

                //final Address address = WalletUtils.getWalletAddressOfReceived(WalletConstants.NETWORK_PARAMETERS,transaction, wallet);
                final Coin amount = transaction.getValue(wallet);
                final TransactionConfidence.ConfidenceType confidenceType = transaction.getConfidence().getConfidenceType();

                if (amount.isGreaterThan(Coin.ZERO)) {
                    //notificationCount++;
                    notificationAccumulatedAmount = notificationAccumulatedAmount.add(amount);

                    Coin notificationAccumulatedAmountTemp = Coin.valueOf(notificationAccumulatedAmount.value);
                    Intent openIntent = new Intent(getApplicationContext(), WalletActivity.class);
                    openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    openPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, openIntent, 0);
                    Intent resultIntent = new Intent(getApplicationContext(), RextWalletService.this.getClass());
                    resultIntent.setAction(ACTION_CANCEL_COINS_RECEIVED);
                    deleteIntent = PendingIntent.getService(RextWalletService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                    mBuilder = new NotificationCompat.Builder(RextWalletService.this, CHANNEL_ID)
                            .setContentTitle("PIV received!")
                            .setContentText("Coins received for a value of " + notificationAccumulatedAmountTemp.toFriendlyString())
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_push_notification_shield)
                            .setColor(ContextCompat.getColor(RextWalletService.this, R.color.bgPurple))
                            .setDeleteIntent(deleteIntent)
                            .setContentIntent(openPendingIntent);
                    nm.notify(NOT_COINS_RECEIVED, mBuilder.build());

                    notificationAccumulatedAmount = Coin.ZERO;
                } else {
                    // TODO: Correct this.. mint txes are going to pass here..
                    log.error("transaction with a value lesser than zero arrives..");
                }

            }catch (Exception e){
                log.error("Something happen on coin receive ", e);
            }

        }
    };

    private TransactionConfidenceEventListener transactionConfidenceEventListener = new TransactionConfidenceEventListener() {
        @Override
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction transaction) {
            org.rextj.core.Context.propagate(CONTEXT);
            try {
                if (transaction != null) {
                    if (transaction.getConfidence().getDepthInBlocks() > 1) {
                        long now = System.currentTimeMillis();
                        if (lastUpdateTime + 5000 < now) {
                            lastUpdateTime = now;
                            // update balance state
                            Intent intent = new Intent(ACTION_NOTIFICATION);
                            intent.putExtra(INTENT_BROADCAST_DATA_TYPE, INTENT_BROADCAST_DATA_ON_COIN_RECEIVED);
                            broadcastManager.sendBroadcast(intent);
                        }
                    }
                }
            }catch (Exception e){
                log.error("onTransactionConfidenceChanged exception",e);
            }
        }
    };

    @Override
    public void onCreate() {
        serviceCreatedAt = System.currentTimeMillis();
        super.onCreate();
        try {
            log.info("REXT service started");
            // Android stuff
            final String lockName = getPackageName() + " blockchain sync";
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
            nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,CHANNEL_ID,NotificationManager.IMPORTANCE_HIGH));
            }
            nm.cancelAll();
            broadcastManager = LocalBroadcastManager.getInstance(this);
            // REXT
            rextApplication = RextApplication.getInstance();
            module = (RextModuleImp) rextApplication.getModule();
            blockchainManager = module.getBlockchainManager();

            // Schedule service
            tryScheduleService();

            peerConnectivityListener = new PeerConnectivityListener();

            File file = getDir("blockstore_v2",MODE_PRIVATE);
            String filename = RextContext.Files.BLOCKCHAIN_FILENAME;
            boolean fileExists = new File(file,filename).exists();
            blockchainStore = new SnappyBlockchainStore(RextContext.CONTEXT,file,filename);
            blockchainManager.init(
                    blockchainStore,
                    file,
                    filename,
                    fileExists
            );

            executor = Executors.newFixedThreadPool(2, new ServiceThreadFactory("WalletService"));

            module.addCoinsReceivedEventListener(executor, coinReceiverListener);
            module.addOnTransactionConfidenceChange(executor, transactionConfidenceEventListener);

            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
            intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
            registerReceiver(connectivityReceiver, intentFilter); // implicitly init PeerGroup


        } catch (Error e){
            e.printStackTrace();
            CrashReporter.appendSavedBackgroundTraces(e);
            Intent intent = new Intent(IntentsConstants.ACTION_STORED_BLOCKCHAIN_ERROR);
            broadcastManager.sendBroadcast(intent);
            throw e;
        } catch (Exception e){
            // todo: I have to handle the connection refused..
            e.printStackTrace();
            CrashReporter.appendSavedBackgroundTraces(e);
            // for now i just launch a notification
            Intent intent = new Intent(IntentsConstants.ACTION_TRUSTED_PEER_CONNECTION_FAIL);
            broadcastManager.sendBroadcast(intent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.info("REXT service onStartCommand");
        try {
            if (intent != null) {
                try {
                    log.info("service init command: " + intent
                            + (intent.hasExtra(Intent.EXTRA_ALARM_COUNT) ? " (alarm count: " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0) + ")" : ""));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("service init command: " + intent
                            + (intent.hasExtra(Intent.EXTRA_ALARM_COUNT) ? " (alarm count: " + intent.getLongArrayExtra(Intent.EXTRA_ALARM_COUNT) + ")" : ""));
                }
                final String action = intent.getAction();
                if (ACTION_SCHEDULE_SERVICE.equals(action)) {
                    check();
                } else if (ACTION_CANCEL_COINS_RECEIVED.equals(action)) {
                    notificationAccumulatedAmount = Coin.ZERO;
                    nm.cancel(NOT_COINS_RECEIVED);
                } else if (ACTION_RESET_BLOCKCHAIN.equals(action)) {
                    log.info("will remove blockchain on service shutdown");
                    resetBlockchainOnShutdown.set(true);
                    stopSelf();
                } else if (ACTION_RESET_BLOCKCHAIN_ROLLBACK_TO.equals(action)){
                    log.info("will remove blockchain on service shutdown");
                    resetBlockchainOnShutdown.set(true);
                    resetToHeight = intent.getIntExtra("height",-1);
                    stopSelf();
                } else if (ACTION_BROADCAST_TRANSACTION.equals(action)) {
                    blockchainManager.broadcastTransaction(intent.getByteArrayExtra(DATA_TRANSACTION_HASH));
                }
            } else {
                log.warn("service restart, although it was started as non-sticky");
            }
        }catch (Exception e){
            log.error("onStartCommand exception",e);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.info(".onDestroy()");
        try {
            // todo: notify module about this shutdown...
            try {
                unregisterReceiver(connectivityReceiver);
            }catch (Exception e){
                // swallow
            }

            if (module.isStarted()) {
                log.info("removing listeners..");
                // remove listeners
                module.removeCoinsReceivedEventListener(coinReceiverListener);
                module.removeTransactionsConfidenceChange(transactionConfidenceEventListener);
                blockchainManager.removeBlockchainDownloadListener(blockchainDownloadListener);

                if (resetToHeight != -1) {
                    log.info("resetting wallet to height " + resetToHeight);
                    blockchainManager.rollbackTo(resetToHeight);
                    resetToHeight = -1;
                    resetBlockchainOnShutdown.set(false);
                } else {
                    log.info("destroying blockchainManager, resetting chain: " + resetBlockchainOnShutdown.get());
                    blockchainManager.destroy(resetBlockchainOnShutdown.getAndSet(false));
                }
            }else {
                tryScheduleServiceNow();
            }

            if (wakeLock.isHeld()) {
                log.debug("wakelock still held, releasing");
                wakeLock.release();
            }

            try {
                // Remove every notification
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancelAll();
            }catch (Exception e){
                log.error("Exception cancelling notifications", e);
            }

            log.info("service was up for " + ((System.currentTimeMillis() - serviceCreatedAt) / 1000 / 60) + " minutes");
            // schedule service it is not scheduled yet
            tryScheduleService();
        }catch (Exception e){
            log.error("onDestroy exception",e);
        }
    }

    public synchronized void broadcastCoinSpendTransactionSync(SendRequest sendRequest){
        if (isSending.compareAndSet(false,true)) {
            log.info("broadcastCoinSpendTransactionSync " + sendRequest);
            Future<Boolean> future = executor.submit(() -> {
                String failMsg;
                boolean showErrorMsg = false;
                try {

                    // Print every acc value that is on the db...
                    ArrayList<StoredAccumulator> list = ((AccStoreDb)RextContext.CONTEXT.accStore).list();
                    log.error("------ Stored accumulators..");
                    for (StoredAccumulator storedAccumulator : list) {
                        log.error(storedAccumulator.toString());
                    }
                    log.error("------ END Stored accumulators..");

                    // Notificate user about this process

                    launchSpendProcessNotification();

                    log.info("Starting spend process..");

                    Transaction tx = module.spendZpiv(RextContext.CONTEXT, sendRequest, blockchainManager.getPeerGroup(), executor);

                    log.info("Spend succeed");

                    Intent intent = new Intent(ACTION_NOTIFICATION);
                    intent.putExtra(INTENT_BROADCAST_DATA_TYPE, INTENT_TX_SENT);
                    intent.putExtra(DATA_TRANSACTION_HASH, tx.getHash());
                    broadcastManager.sendBroadcast(intent);

                    Intent openIntent = new Intent(RextWalletService.this, WalletActivity.class);
                    openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    openIntent.putExtra("Private", true);
                    PendingIntent openPendingIntent = PendingIntent.getActivity(RextWalletService.this, 0, openIntent,  PendingIntent.FLAG_CANCEL_CURRENT);

                    Coin value = module.getValueSentFromMe(tx, false);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("zPIV send completed")
                            .setContentText(String.format("Amount %s", value.toFriendlyString()))
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_push_notification_shield)
                            .setColor(ContextCompat.getColor(RextWalletService.this, R.color.bgPurple))
                            .setContentIntent(openPendingIntent);
                    nm.notify(NOT_ZPIV_SENT_COMPLETED, mBuilder.build());

                    isSending.set(false);
                    stopSpendProcessNotification();
                    return true;
                } catch (InsufficientMoneyException e) {
                    log.warn("############ - Cannot spend coins", e);
                    failMsg = e.getMessage();
                } catch (CannotSpendCoinsException e) {
                    log.warn("############ - Cannot spend coins", e);
                    failMsg = e.getMessage();
                }catch (RequestFailedErrorcodeException e){
                    log.warn("############ - Cannot spend coins", e);
                    showErrorMsg = true;
                    failMsg = e.getMessage();
                } catch (Exception e){
                    log.warn("############ - Cannot spend coins", e);
                    showErrorMsg = true;
                    failMsg = e.getMessage();
                }
                isSending.set(false);
                stopSpendProcessNotification();

                Intent intent = new Intent(ACTION_NOTIFICATION);
                intent.putExtra(INTENT_BROADCAST_DATA_TYPE, INTENT_TX_FAIL);
                broadcastManager.sendBroadcast(intent);


                Intent openIntent = new Intent(RextWalletService.this, WalletActivity.class);
                openIntent.putExtra("Private", true);
                PendingIntent openPendingIntent = PendingIntent.getActivity(RextWalletService.this, 0, openIntent, 0);

                String finalMsg = "Please try again in few minutes";
                if (showErrorMsg){
                    finalMsg = failMsg;
                }

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("zPIV spend not completed")
                        .setContentText(finalMsg)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_push_notification_shield)
                        .setColor(ContextCompat.getColor(RextWalletService.this, R.color.bgPurple))
                        .setContentIntent(openPendingIntent);
                nm.notify(NOT_ZPIV_SEND_FAILED, mBuilder.build());

                // Do it twice just for android issues.
                stopSpendProcessNotification();

                return true;
            });
            //Futures.
        }else {
            throw new IllegalStateException("Wallet already trying to spend a coin, wait until this process is finished please");
        }
    }

    private void launchSpendProcessNotification() {

        android.support.v4.app.NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_push_notification_shield)
                .setContentTitle(getString(R.string.spending_zpiv))
                .setAutoCancel(false)
                .setOngoing(true)
                .setProgress(0, 0 , true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(RextWalletService.this,R.color.bgPurple));

        nm.notify(NOT_SPENDING_PROCESS, mBuilder.build());
    }

    private void stopSpendProcessNotification(){
        nm.cancel(NOT_SPENDING_PROCESS);
    }


    /**
     * Schedule service for later
     */
    private void tryScheduleService() {
        boolean isSchedule = System.currentTimeMillis() < module.getConf().getScheduledBLockchainService();

        if (!isSchedule){
            AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
            long scheduleTime = System.currentTimeMillis() + 1000 * 60 * 15;//(1000 * 60 * 60); // One hour from now
            log.info("scheduling service for " + new SimpleDateFormat("MMM dd, yyyy hh:mm a").format(scheduleTime));
            Intent intent = new Intent(this, RextWalletService.class);
            intent.setAction(ACTION_SCHEDULE_SERVICE);
            alarm.set(
                    // This alarm will wake up the device when System.currentTimeMillis()
                    // equals the second argument value
                    AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    // PendingIntent.getService creates an Intent that will start a service
                    // when it is called. The first argument is the Context that will be used
                    // when delivering this intent. Using this has worked for me. The second
                    // argument is a request code. You can use this code to cancel the
                    // pending intent if you need to. Third is the intent you want to
                    // trigger. In this case I want to create an intent that will start my
                    // service. Lastly you can optionally pass flags.
                    PendingIntent.getService(this, 0,intent , 0)
            );
            // save
            module.getConf().saveScheduleBlockchainService(scheduleTime);
        }
    }


    private void tryScheduleServiceNow() {
        log.info("scheduling service now");
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        long scheduleTime = System.currentTimeMillis() + 1000 * 60 * 10; // 2 minutes

        Intent intent = new Intent(this, RextWalletService.class);
        intent.setAction(ACTION_SCHEDULE_SERVICE);
        alarm.set(
                // This alarm will wake up the device when System.currentTimeMillis()
                // equals the second argument value
                AlarmManager.RTC_WAKEUP,
                scheduleTime,
                // PendingIntent.getService creates an Intent that will start a service
                // when it is called. The first argument is the Context that will be used
                // when delivering this intent. Using this has worked for me. The second
                // argument is a request code. You can use this code to cancel the
                // pending intent if you need to. Third is the intent you want to
                // trigger. In this case I want to create an intent that will start my
                // service. Lastly you can optionally pass flags.
                PendingIntent.getService(this, 0,intent , 0)
        );
    }

    private void requestRateCoin(){
        final AppConf appConf = rextApplication.getAppConf();
        RextRate rextRate = module.getRate(appConf.getSelectedRateCoin());
        if (rextRate == null || rextRate.getTimestamp() + RextContext.RATE_UPDATE_TIME < System.currentTimeMillis()){
            new Thread(() -> {
                try {
                    CoinMarketCapApiClient c = new CoinMarketCapApiClient();
                    CoinMarketCapApiClient.RextMarket rextMarket = c.getRextPxrice();
                    RextRate rextRate1 = new RextRate("USD",rextMarket.priceUsd,System.currentTimeMillis());
                    module.saveRate(rextRate1);
                    final RextRate rextBtcRate = new RextRate("BTC",rextMarket.priceBtc,System.currentTimeMillis());
                    module.saveRate(rextBtcRate);

                    // Get the rest of the rates:
                    List<RextRate> rates = new CoinMarketCapApiClient.BitPayApi().getRates((code, name, bitcoinRate) -> {
                        BigDecimal rate = bitcoinRate.multiply(rextBtcRate.getRate());
                        return new RextRate(code,rate,System.currentTimeMillis());
                    });

                    for (RextRate rate : rates) {
                        module.saveRate(rate);
                    }

                } catch (RequestRextRateException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private AtomicBoolean isChecking = new AtomicBoolean(false);

    /**
     * Check and download the blockchain if it needed
     */
    private void check() {
        log.info("check");
        Log.i("REXT_SERVICE", "Running check, is checking: " + isChecking.get());
        try {
            if (!isChecking.getAndSet(true)) {

                if(module.isStarted()) {
                    blockchainManager.check(
                            impediments,
                            peerConnectivityListener,
                            peerConnectivityListener,
                            blockchainDownloadListener,
                            null,
                            executor
                    );
                }else {
                    tryScheduleServiceNow();
                }
                //todo: ver si conviene esto..
                broadcastBlockchainState(true);
                isChecking.set(false);
            }
        }catch (Exception e){
            log.error("Exception on blockchainManager check", e);
            isChecking.set(false);
            broadcastBlockchainState(false);
            // Try to schedule the service again
            tryScheduleServiceNow();
        }
    }

    private void broadcastBlockchainState(boolean isCheckOk) {
        boolean showNotif = false;
        if (!impediments.isEmpty()) {

            StringBuilder stringBuilder = new StringBuilder();
            for (Impediment impediment : impediments) {
                if (stringBuilder.length() != 0){
                    stringBuilder.append("\n");
                }
                if (impediment == Impediment.NETWORK){
                    blockchainState = BlockchainState.NOT_CONNECTION;
                    stringBuilder.append("No peer connection");
                }else if(impediment == Impediment.STORAGE){
                    stringBuilder.append("No available storage");
                    showNotif = true;
                }
            }

            if(showNotif) {
                android.support.v4.app.NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_push_notification_shield)
                                .setContentTitle("Alert")
                                .setContentText(stringBuilder.toString())
                                .setAutoCancel(true)
                                .setColor(ContextCompat.getColor(RextWalletService.this,R.color.bgPurple))
                        ;

                nm.notify(NOT_BLOCKCHAIN_ALERT, mBuilder.build());
            }
        }

        if (isCheckOk){
            broadcastBlockchainStateIntent();
        }
    }

    private void broadcastBlockchainStateIntent(){
        final long now = System.currentTimeMillis();
        if (now-lastMessageTime> TimeUnit.SECONDS.toMillis(6)) {
            lastMessageTime = System.currentTimeMillis();
            Intent intent = new Intent(ACTION_NOTIFICATION);
            intent.putExtra(INTENT_BROADCAST_DATA_TYPE, INTENT_BROADCAST_DATA_BLOCKCHAIN_STATE);
            intent.putExtra(INTENT_EXTRA_BLOCKCHAIN_STATE,blockchainState);
            broadcastManager.sendBroadcast(intent);
        }
    }

    private void broadcastPeerConnected() {
        Intent intent = new Intent(ACTION_NOTIFICATION);
        intent.putExtra(INTENT_BROADCAST_DATA_TYPE, INTENT_BROADCAST_DATA_PEER_CONNECTED);
        broadcastManager.sendBroadcast(intent);
    }

}
