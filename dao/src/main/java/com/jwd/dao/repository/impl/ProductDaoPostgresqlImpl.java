package com.jwd.dao.repository.impl;

import com.jwd.dao.connection.impl.ConnectionPoolImpl;
import com.jwd.dao.domain.Pageable;
import com.jwd.dao.domain.ProductRow;
import com.jwd.dao.exception.DaoException;
import com.jwd.dao.repository.ProductDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProductDaoPostgresqlImpl extends AbstractDao implements ProductDao {
    private static final String COUNT_ALL_FILTERED_SORTED = "SELECT count(p.id) FROM products p;";
    // SELECT * FROM products p ORDER BY p.name ASC LIMIT 5 OFFSET 0;
    private static final String FIND_PAGE_FILTERED_SORTED = "SELECT * FROM products p ORDER BY p.%s %s LIMIT ? OFFSET ?;";
    public ProductDaoPostgresqlImpl(final ConnectionPoolImpl connectionPool) {
        super(connectionPool);
    }

    @Override
    public Pageable<ProductRow> findPageByFilter(Pageable<ProductRow> daoProductPageable) throws DaoException {
        final int offset = (daoProductPageable.getPageNumber() - 1) * daoProductPageable.getLimit();
        List<Object> parameters1 = Collections.emptyList(); // todo implement filtering
        List<Object> parameters2 = Arrays.asList( // todo implement filtering
                daoProductPageable.getLimit(),
                offset
        );
        Connection connection = null;
        PreparedStatement preparedStatement1 = null;
        PreparedStatement preparedStatement2 = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        try {
            connection = getConnection(false);
            preparedStatement1 = getPreparedStatement(COUNT_ALL_FILTERED_SORTED, connection, parameters1);
            final String findPageOrderedQuery =
                    String.format(FIND_PAGE_FILTERED_SORTED, daoProductPageable.getSortBy(), daoProductPageable.getDirection());
            preparedStatement2 = getPreparedStatement(findPageOrderedQuery, connection, parameters2);
            resultSet1 = preparedStatement1.executeQuery();
            resultSet2 = preparedStatement2.executeQuery();
            connection.commit();

            return getProductRowPageable(daoProductPageable, resultSet1, resultSet2);
        } catch (SQLException | DaoException e) {
            e.printStackTrace();
            throw new DaoException(e);
        } finally {
            close(resultSet1, resultSet2);
            close(preparedStatement1, preparedStatement2);
            retrieve(connection);
        }
    }

    private Pageable<ProductRow> getProductRowPageable(Pageable<ProductRow> daoProductPageable,
                                                       ResultSet resultSet1,
                                                       ResultSet resultSet2) throws SQLException {
        final Pageable<ProductRow> pageable = new Pageable<>();
        long totalElements = 0L;
        while (resultSet1.next()) {
            totalElements = resultSet1.getLong(1);
        }
        final List<ProductRow> rows = new ArrayList<>();
        while (resultSet2.next()) {
            long id = resultSet2.getLong(1);
            String type = resultSet2.getString(6);
            String company = resultSet2.getString(7);
            String name = resultSet2.getString(8);
            rows.add(new ProductRow(id, type, company, name));
        }
        pageable.setPageNumber(daoProductPageable.getPageNumber());
        pageable.setLimit(daoProductPageable.getLimit());
        pageable.setTotalElements(totalElements);
        pageable.setElements(rows);
        pageable.setFilter(daoProductPageable.getFilter());
        pageable.setSortBy(daoProductPageable.getSortBy());
        pageable.setDirection(daoProductPageable.getDirection());
        return pageable;
    }
}
