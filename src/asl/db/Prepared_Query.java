package asl.db;

import com.sun.rowset.CachedRowSetImpl;

import javax.sql.rowset.CachedRowSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Prepared_Query {
    private final static Logger lgr = Logger.getLogger(Prepared_Query.class
            .getName());
    private Connection con = null;
    private String stm = null;
    private PreparedStatement pst = null;

    public Prepared_Query(String stm) throws SQLException {
        this.stm = stm;
    }

    public ResultSet execute() {
        ResultSet rs = null;
        CachedRowSet row_set = null;
        try {
            this.con.setAutoCommit(false);
            rs = this.pst.executeQuery();
            row_set = new CachedRowSetImpl();
            row_set.populate(rs);
        } catch (SQLException ex) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex1) {
                    Logger lgr = Logger.getLogger(Query.class.getName());
                    lgr.log(Level.WARNING, ex1.getMessage(), ex1);
                }
            }
            lgr.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException ex) {
                lgr.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return row_set;
    }

    public ResultSet no_trans_execute() {
        ResultSet rs = null;
        CachedRowSet row_set = null;
        try {
            this.con.setAutoCommit(true);
            rs = this.pst.executeQuery();
            row_set = new CachedRowSetImpl();
            row_set.populate(rs);
        } catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (con != null)
                    con.close();
            } catch (SQLException ex) {
                lgr.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return row_set;
    }

    public PreparedStatement get_pst() {
        return this.pst;
    }

    public Connection getCon() {
        return con;
    }

    public void setCon(Connection con) {
        this.con = con;
        try {
            this.con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            this.pst = this.con.prepareCall(this.stm);
        } catch (SQLException e) {
            lgr.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}