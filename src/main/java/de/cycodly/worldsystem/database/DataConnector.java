package de.cycodly.worldsystem.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import de.cycodly.worldsystem.WorldSystemPlugin;
import lombok.Getter;

public abstract class DataConnector implements IDataUtil {

    final Object lock = new Object();
    @Getter
    Connection connection;

    public void close() {
        synchronized (lock) {
            try {
                if (connection == null || connection.isClosed()) {
                    WorldSystemPlugin.logger().log(Level.SEVERE, "[WorldSystem | DB] Connection does not exist or was already closed");
                    return;
                }
                connection.close();
            } catch (SQLException e) {
                WorldSystemPlugin.logger().log(Level.WARNING, "[WorldSystem | DB] Connection could not be closed");
                e.printStackTrace();
            }
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        synchronized (lock) {
            if (connection == null || connection.isClosed())
                connect();
            return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
    }

    public ResultSet executeQuery(PreparedStatement ps) throws SQLException {
        synchronized (lock) {
            if (connection == null || connection.isClosed())
                connect();
            return ps.executeQuery();
        }
    }

    public int executeUpdate(PreparedStatement ps) throws SQLException {
        synchronized (lock) {
            if (connection == null || connection.isClosed())
                connect();
            return ps.executeUpdate();
        }
    }

    @Override
    public boolean isConnectionAvailable() {
        return connection != null;
    }
}
