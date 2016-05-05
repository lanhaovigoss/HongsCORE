package app.hongs.db.link;

import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 简单连接器
 * @author Hongs
 */
public class Simple extends Link {

    private final String     jdbc;
    private final String     path;
    private final Properties info;

    public  Simple(String jdbc, String name, Properties info)
            throws HongsException {
        super(name);

        this.jdbc = jdbc;
        this.path = name;
        this.info = info;
    }

    public  Simple(String jdbc, String name)
            throws HongsException {
        this( jdbc, name, new Properties() );
    }

    @Override
    public  Connection connect()
            throws HongsException {
        try {
            if (connection == null || connection.isClosed()) {
                connection  = connect( jdbc , path , info );
                
                if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                    CoreLogger.trace("DB: Connect to '"+name+"' by simple mode: "+jdbc+" "+path);
                }
            }

            initial(); // 预置

            return connection;
        } catch (SQLException ex) {
            throw new HongsException(0x1024, ex);
        } catch (ClassNotFoundException ex ) {
            throw new HongsException(0x1024, ex);
        }
    }

    public  static Connection connect(String jdbc, String name, Properties info)
            throws SQLException, ClassNotFoundException {
        Class.forName(jdbc);

        if (info != null && ! info.isEmpty()) {
            if (info.containsKey("username")) {
                info.put ( "user" , info.get("username") );
            }
            return DriverManager.getConnection(name, info);
        } else {
            return DriverManager.getConnection(name);
        }
    }

}
