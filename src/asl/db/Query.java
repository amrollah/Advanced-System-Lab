package asl.db;

import com.sun.rowset.CachedRowSetImpl;

import javax.sql.rowset.CachedRowSet;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Query {
    private final static Logger lgr = Logger.getLogger(Query.class.getName());
    private static FileHandler handler = null;
    private Connection con = null;

    public Query(Connection con) {
        this.con = con;
        try {
            this.con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet execute(String query) {
        Statement st = null;
        ResultSet rs = null;
        CachedRowSet row_set = null;
        try {
            st = this.con.createStatement();
            try {
                rs = st.executeQuery(query);
            } catch (SQLException ex) {
                try {
                    rs = st.executeQuery(query);
                } catch (SQLException ex2) {
                    rs = st.executeQuery(query);
                }
            }
            row_set = new CachedRowSetImpl();
            row_set.populate(rs);
        } catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (st != null)
                    st.close();
                if (con != null)
                    con.close();
            } catch (SQLException ex) {
                lgr.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return row_set;
    }

    public ResultSet no_trans_execute(String query) {
        Statement st = null;
        ResultSet rs = null;
        CachedRowSet row_set = null;
        try {
            this.con.setAutoCommit(true);
            st = this.con.createStatement();
            rs = st.executeQuery(query);
            row_set = new CachedRowSetImpl();
            row_set.populate(rs);
        } catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (st != null)
                    st.close();
                if (con != null)
                    con.close();
            } catch (SQLException ex) {
                lgr.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return row_set;
    }

    public Connection getCon() {
        return con;
    }
}