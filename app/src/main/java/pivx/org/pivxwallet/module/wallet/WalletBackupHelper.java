package rext.org.rextwallet.module.wallet;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import rext.org.rextwallet.RextApplication;
import rext.org.rextwallet.module.RextContext;
import global.utils.Iso8601Format;
import global.BackupHelper;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by furszy on 6/29/17.
 */

public class WalletBackupHelper implements BackupHelper{

    public File determineBackupFile(String extraData) {
        RextContext.Files.EXTERNAL_WALLET_BACKUP_DIR.mkdirs();
        checkState(RextContext.Files.EXTERNAL_WALLET_BACKUP_DIR.isDirectory(), "%s is not a directory", RextContext.Files.EXTERNAL_WALLET_BACKUP_DIR);

        final DateFormat dateFormat = Iso8601Format.newDateFormat();
        dateFormat.setTimeZone(TimeZone.getDefault());

        String appName = RextApplication.getInstance().getVersionName();

        for (int i = 0; true; i++) {
            final StringBuilder filename = new StringBuilder(RextContext.Files.getExternalWalletBackupFileName(appName));
            filename.append('-');
            filename.append(dateFormat.format(new Date()));
            if (extraData!=null){
                filename.append("-"+extraData);
            }
            if (i > 0)
                filename.append(" (").append(i).append(')');

            final File file = new File(RextContext.Files.EXTERNAL_WALLET_BACKUP_DIR, filename.toString());
            if (!file.exists())
                return file;
        }
    }

}
