package rext.org.rextwallet.module;

import android.os.Environment;
import android.text.format.DateUtils;

import com.zerocoinj.core.context.ZerocoinContext;

import org.rextj.core.Context;
import org.rextj.core.NetworkParameters;
import org.rextj.params.MainNetParams;
import org.rextj.params.TestNet3Params;

import java.io.File;

import rext.org.rextwallet.AndroidJniBridge;

/**
 * Created by furszy on 6/4/17.
 */

public class RextContext {

    public static final boolean IS_TEST = false;
    public static final NetworkParameters NETWORK_PARAMETERS = IS_TEST? TestNet3Params.get(): MainNetParams.get();
    /** Rextj global context. */
    public static final Context CONTEXT = new Context(NETWORK_PARAMETERS);

    // Zerocoin wallet features activation.
    public static final boolean IS_ZEROCOIN_WALLET_ACTIVE = false;

    // Peers dns discovery
    public static final boolean IS_DNS_DISCOVERY_ENABLED_BY_DEFAULT = true;

    {
        CONTEXT.zerocoinContext = new ZerocoinContext(new AndroidJniBridge());
    }

    public static final String DEFAULT_RATE_COIN = "USD";
    public static final long RATE_UPDATE_TIME = 72000000;

    public static final String ENABLE_BIP44_APP_VERSION = "1.03";

    /** REXT wallet released time */
    public static final long REXT_WALLET_APP_RELEASED_ON_PLAY_STORE_TIME = 1500940800;

    /** Currency exchange rate */
    public static final String URL_FIAT_CURRENCIES_RATE = "https://bitpay.com/rates";

    // report mail
    public static final String REPORT_EMAIL = "furszy@rext.org";
    /** Subject line for manually reported issues. */
    public static final String REPORT_SUBJECT_ISSUE = "Reported issue";

    /** Donation address */
    public static final String DONATE_ADDRESS = "DLwFC1qQbUzFZJg1vnvdAXBunRPh6anceK";

    public static final class Files{

        private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId();

        public static final String BIP39_WORDLIST_FILENAME = "bip39-wordlist.txt";
        /** Filename of the block store for storing the chain. */
        public static final String BLOCKCHAIN_FILENAME = "blockchain" + FILENAME_NETWORK_SUFFIX;
        /** Filename of the wallet. */
        public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;
        /** How often the wallet is autosaved. */
        public static final long WALLET_AUTOSAVE_DELAY_MS = 5 * DateUtils.SECOND_IN_MILLIS;
        /** Filename of the automatic wallet backup. */
        public static final String WALLET_KEY_BACKUP_PROTOBUF = "key-backup-protobuf" + FILENAME_NETWORK_SUFFIX;
        /** Path to external storage */
        public static final File EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory();
        /** Filename of the manual wallet backup. */
        public static final String EXTERNAL_WALLET_BACKUP = "rext-wallet-backup" +"_"+ FILENAME_NETWORK_SUFFIX;
        /** Manual backups go here. */
        public static final File EXTERNAL_WALLET_BACKUP_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        public static final String getExternalWalletBackupFileName(String appName){
            return appName+"_"+EXTERNAL_WALLET_BACKUP;
        }
        /** Checkpoint filename */
        public static final String CHECKPOINTS_FILENAME = "checkpoints";

    }

    /** Minimum memory */
    public static final int MEMORY_CLASS_LOWEND = 48;

    public static final int PEER_DISCOVERY_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;
    public static final int PEER_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;

    /** Maximum size of backups. Files larger will be rejected. */
    public static final long BACKUP_MAX_CHARS = 10000000;

    /** MAX TIME WAITED TO SAY THAT THE APP IS NOT SYNCED ANYMORE.. in millis*/
    public static final long OUT_OF_SYNC_TIME = 60000; // 1 minute
}
