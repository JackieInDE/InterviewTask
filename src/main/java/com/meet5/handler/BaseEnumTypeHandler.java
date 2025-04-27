package com.meet5.handler;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Method;
import java.sql.*;
public abstract class BaseEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;
    private final Method fromCodeMethod;

    public BaseEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
        try {
            this.fromCodeMethod = type.getMethod("fromCode", int.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Enum class " + type.getName() + " must have a static fromCode(int) method", e);
        }
    }

    @SuppressWarnings("unchecked")
    private E convert(int code) {
        try {
            return (E) fromCodeMethod.invoke(null, code);
        } catch (Exception e) {
            throw new RuntimeException("Cannot convert code " + code + " to " + type.getSimpleName(), e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        try {
            Method getCode = parameter.getClass().getMethod("getCode");
            Object code = getCode.invoke(parameter);
            ps.setObject(i, code);
        } catch (Exception e) {
            throw new SQLException("Cannot get code from enum " + parameter.getClass().getSimpleName(), e);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return convert(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        if (rs.wasNull()) {
            return null;
        }
        return convert(code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        if (cs.wasNull()) {
            return null;
        }
        return convert(code);
    }
}

