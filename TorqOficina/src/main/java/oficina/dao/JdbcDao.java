package oficina.dao;

import oficina.exception.RegraNegocioException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Operações JDBC parametrizadas. A conexão pertence à transação do chamador. */
public final class JdbcDao {
    public static List<Map<String, Object>> query(Connection db, String sql, Object... args)
            throws SQLException {
        try (var st = db.prepareStatement(sql)) {
            bind(st, args);
            try (var rs = st.executeQuery()) {
                var list = new ArrayList<Map<String, Object>>();
                var meta = rs.getMetaData();
                while (rs.next()) {
                    var row = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof java.sql.Timestamp || v instanceof java.sql.Date)
                            v = v.toString();
                        row.put(meta.getColumnLabel(i), v);
                    }
                    list.add(row);
                }
                return list;
            }
        }
    }

    public static Map<String, Object> one(Connection db, String sql, Object... args)
            throws SQLException {
        var q = query(db, sql, args);
        if (q.isEmpty()) throw new RegraNegocioException(404, "Registro não encontrado.");
        return q.get(0);
    }

    public static void bind(PreparedStatement st, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) st.setObject(i + 1, args[i]);
    }

    public static long insert(Connection db, String sql, Object... args) throws SQLException {
        try (var st = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(st, args);
            st.executeUpdate();
            try (var rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                return 0;
            }
        }
    }

    public static void update(Connection db, String sql, Object... args) throws SQLException {
        try (var st = db.prepareStatement(sql)) {
            bind(st, args);
            st.executeUpdate();
        }
    }

    public static long number(Object value) {
        return ((Number) value).longValue();
    }
}
