package rext.org.rextwallet.module.store;

import android.support.annotation.Nullable;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import com.zerocoinj.core.CoinDenomination;
import com.zerocoinj.core.accumulators.Accumulator;

import org.rextj.core.Block;
import org.rextj.core.Context;
import org.rextj.core.NetworkParameters;
import org.rextj.core.Sha256Hash;
import org.rextj.core.StoredBlock;
import org.rextj.core.Utils;
import org.rextj.store.BlockStore;
import org.rextj.store.BlockStoreException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import host.furszy.zerocoinj.store.AccStore;
import host.furszy.zerocoinj.store.AccStoreException;

/**
 * Created by furszy on 10/17/17.
 */

public class SnappyAccStore implements AccStore{

    private DB db;
    private final File path;
    private final String filename;


    public SnappyAccStore(Context context, File directory, String filename) throws BlockStoreException {
        this.path = directory;
        this.filename = filename;
        try {
            tryOpen(directory, filename);
        } catch (IOException e) {
            throw new BlockStoreException(e);
        }
    }

    private synchronized void tryOpen(File directory,String filename) throws IOException, BlockStoreException {
        try {
            db = DBFactory.open(directory.getAbsolutePath(),filename);
        } catch (SnappydbException e) {
            throw new IOException(e);
        }
    }

    public synchronized void put(int height, Accumulator acc) throws AccStoreException {
        try {
            this.db.put(this.toKey(height, acc.getDenomination()), Utils.serializeBigInteger(acc.getValue()));
        } catch (SnappydbException e) {
            throw new AccStoreException(e);
        }
    }

    public synchronized BigInteger get(int height, CoinDenomination denom) throws AccStoreException {
        byte[] data;
        try {
            data = this.db.getBytes(this.toKey(height, denom));
        } catch (SnappydbException e) {
            throw new AccStoreException(e);
        }
        return data == null ? null : Utils.unserializeBiginteger(data);
    }

    private String toKey(int height, CoinDenomination denom) {
        return Hex.toHexString(ByteBuffer.allocate(8).putInt(height).putInt(denom.getDenomination()).array());
    }

    public synchronized void close() throws BlockStoreException {
        try {
            db.destroy();
        } catch (SnappydbException e) {
            throw new BlockStoreException(e);
        }
    }

    public File getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }

    public void truncate() {
        try {
            db.destroy();
        } catch (SnappydbException e) {
            throw new RuntimeException(e);
        }
    }
}
