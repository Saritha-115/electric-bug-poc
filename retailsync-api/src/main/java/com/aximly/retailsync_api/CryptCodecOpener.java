package com.aximly.retailsync_api;

import java.io.File;
import java.io.IOException;
import net.ucanaccess.jdbc.JackcessOpenerInterface;
import com.healthmarketscience.jackcess.CryptCodecProvider;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;

public class CryptCodecOpener implements JackcessOpenerInterface {
    @Override
    public Database open(File fl, String pwd) throws IOException {
        DatabaseBuilder dbd = new DatabaseBuilder(fl);
        dbd.setCodecProvider(new CryptCodecProvider(pwd == null ? "" : pwd));
        dbd.setAutoSync(true);
        dbd.setReadOnly(true); // UCanAccess is read-only again — writes now go through MdbWriter.exe (OLE DB bridge)
        return dbd.open();
    }
}