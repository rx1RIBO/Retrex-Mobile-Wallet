package rext.org.rextwallet.module;

import android.content.SharedPreferences;

import org.rextj.core.Context;
import org.rextj.core.NetworkParameters;

import global.WalletConfiguration;
import rext.org.rextwallet.utils.Configurations;

import static rext.org.rextwallet.module.RextContext.CONTEXT;
import static rext.org.rextwallet.module.RextContext.Files.BLOCKCHAIN_FILENAME;
import static rext.org.rextwallet.module.RextContext.Files.CHECKPOINTS_FILENAME;
import static rext.org.rextwallet.module.RextContext.Files.WALLET_FILENAME_PROTOBUF;
import static rext.org.rextwallet.module.RextContext.Files.WALLET_KEY_BACKUP_PROTOBUF;
import static rext.org.rextwallet.module.RextContext.NETWORK_PARAMETERS;
import static rext.org.rextwallet.module.RextContext.PEER_DISCOVERY_TIMEOUT_MS;
import static rext.org.rextwallet.module.RextContext.PEER_TIMEOUT_MS;

/**
 * Created by furszy on 6/4/17.
 */

public class WalletConfImp extends Configurations implements WalletConfiguration {

    private static final String PREF_TRUSTED_NODE = "trusted_node";
    private static final String PREF_TRUSTED_NODE_PORT = "trusted_node_port";
    private static final String PREFS_KEY_SCHEDULE_BLOCKCHAIN_SERVICE = "sch_block_serv";
    private static final String PREF_CURRENCY_RATE = "currency_code";
    private static final String PREF_BEST_CHAIN_HEIGHT = "best_chain_height";
    private static final String IS_DNS_DISCOVERY_ENABLED = "is_dns_discovery";

    public WalletConfImp(SharedPreferences prefs) {
        super(prefs);
    }

    public void setDSNDiscovery(boolean isEnabled) {
        save(IS_DNS_DISCOVERY_ENABLED,isEnabled);
    }

    public boolean isDNSDiscoveryEnabled(){
        return getBoolean(IS_DNS_DISCOVERY_ENABLED, RextContext.IS_DNS_DISCOVERY_ENABLED_BY_DEFAULT);
    }

    @Override
    public String getTrustedNodeHost() {
        return getString(PREF_TRUSTED_NODE,null);
    }

    @Override
    public void saveTrustedNode(String host, int port) {
        save(PREF_TRUSTED_NODE,host);
        save(PREF_TRUSTED_NODE_PORT,port);
    }

    @Override
    public void cleanTrustedNode() {
        remove(PREF_TRUSTED_NODE);
        remove(PREF_TRUSTED_NODE_PORT);
    }

    @Override
    public void saveScheduleBlockchainService(long time){
        save(PREFS_KEY_SCHEDULE_BLOCKCHAIN_SERVICE,time);
    }

    @Override
    public long getScheduledBLockchainService(){
        return getLong(PREFS_KEY_SCHEDULE_BLOCKCHAIN_SERVICE,0);
    }

    @Override
    public int getTrustedNodePort() {
        return getInt(PREF_TRUSTED_NODE_PORT,RextContext.NETWORK_PARAMETERS.getPort());
    }

    @Override
    public String getMnemonicFilename() {
        return RextContext.Files.BIP39_WORDLIST_FILENAME;
    }

    @Override
    public String getWalletProtobufFilename() {
        return WALLET_FILENAME_PROTOBUF;
    }

    @Override
    public NetworkParameters getNetworkParams() {
        return RextContext.NETWORK_PARAMETERS;
    }

    @Override
    public String getKeyBackupProtobuf() {
        return WALLET_KEY_BACKUP_PROTOBUF;
    }

    @Override
    public long getWalletAutosaveDelayMs() {
        return RextContext.Files.WALLET_AUTOSAVE_DELAY_MS;
    }

    @Override
    public Context getWalletContext() {
        return CONTEXT;
    }

    @Override
    public String getBlockchainFilename() {
        return BLOCKCHAIN_FILENAME;
    }

    @Override
    public String getCheckpointFilename() {
        return CHECKPOINTS_FILENAME;
    }

    @Override
    public int getPeerTimeoutMs() {
        return PEER_TIMEOUT_MS;
    }

    @Override
    public long getPeerDiscoveryTimeoutMs() {
        return PEER_DISCOVERY_TIMEOUT_MS;
    }

    @Override
    public int getMinMemoryNeeded() {
        return RextContext.MEMORY_CLASS_LOWEND;
    }

    @Override
    public long getBackupMaxChars() {
        return RextContext.BACKUP_MAX_CHARS;
    }

    @Override
    public boolean isTest() {
        return RextContext.IS_TEST;
    }

    @Override
    public int getProtocolVersion() {
        return NETWORK_PARAMETERS.getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT);
    }

    @Override
    public void maybeIncrementBestChainHeightEver(int lastBlockSeenHeight) {
        save(PREF_BEST_CHAIN_HEIGHT, lastBlockSeenHeight);
    }

    public int getBestChainHeightEver(){
        return getInt(PREF_BEST_CHAIN_HEIGHT,0);
    }

}
