package com.smartcity.dao;

import com.smartcity.domain.User;
import com.smartcity.exceptions.DbOperationException;
import com.smartcity.exceptions.NotFoundException;
import com.smartcity.mapper.UserMapper;
import com.smartcity.utils.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private JdbcTemplate jdbcTemplate;
    private UserMapper mapper;
    private Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserDaoImpl(DataSource dataSource, UserMapper mapper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.passwordEncoder = new EncryptionUtil();
        this.mapper = mapper;
    }

    @Override
    public User create(User user) {
        try {
            LocalDateTime currentDate = LocalDateTime.now();
            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            jdbcTemplate.update(con -> createStatement(user, currentDate, encryptedPassword, con), holder);
            user.setCreatedDate(currentDate);
            user.setUpdatedDate(currentDate);
            user.setId(Optional.ofNullable(holder.getKey()).map(Number::longValue)
                    .orElseThrow(() -> new DbOperationException("Create user Dao method error: " +
                            "Autogenerated key is null")));
            return user;
        }
        catch (Exception e) {
            logger.error("Create user (id = {}) exception. Message: {}", user.getId(), e.getMessage());
            throw new DbOperationException("Create user exception");
        }
    }


    @Override
    public User findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_ID,
                    mapper, (Long) id);
        }
        catch (EmptyResultDataAccessException ex) {
            throw getAndLogUserNotFoundException(id);
        }
        catch (Exception e) {
            logger.error("Get user (id = {}) exception. Message: {}", id, e.getMessage());
            throw new DbOperationException("Get user exception");
        }
    }

    @Override
    public List<User> findAll(int pageId, int total) {

        String limitUsers = "SELECT * FROM Users LIMIT "+(pageId-1)+","+total;

        try {
            return jdbcTemplate.query(limitUsers, mapper);
        }
        catch (Exception e) {
            logger.error("Get user all users exception. Message: {}", e.getMessage());
            throw new DbOperationException("Get all users exception");
        }
    }

    @Override
    public User findByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_EMAIL,
                    mapper, email);
        }
        catch (EmptyResultDataAccessException ex) {
            NotFoundException notFoundException = new NotFoundException("User not found");
            logger.error("Runtime exception. User not found (email = {}). Message: {}",
                    email, notFoundException.getMessage());

            throw notFoundException;
        }
        catch (Exception e) {
            logger.error("Get user (email = {}) exception. Message: {}", email, e.getMessage());
            throw new DbOperationException("Get user exception");
        }
    }

    @Override
    public List<User> findByOrganizationId(Long organizationId) {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_USERS_BY_ORGANIZATION_ID, mapper, organizationId);
        }
        catch (Exception e) {
            logger.error("Find users by organization id exception. Message: {}", e.getMessage());
            throw new DbOperationException("Find users by organization id exception");
        }
    }

    @Override
    public List<User> findByRoleId(Long roleId) {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_USERS_BY_ROLES_IDS, mapper, roleId);
        }
        catch (Exception e) {
            logger.error("Find users by roles ids exception. Message: {}", e.getMessage());
            throw new DbOperationException("Find users by roles ids exception");
        }
    }

    @Override
    public User update(User user) {
        int rowsAffected;
        try {
            LocalDateTime updatedDate = LocalDateTime.now();
            rowsAffected = jdbcTemplate.update(
                    Queries.SQL_UPDATE_USER,
                    user.getEmail(),
                    user.getSurname(),
                    user.getName(),
                    user.getPhoneNumber(),
                    user.isActive(),
                    updatedDate,
                    user.getId());

            user.setUpdatedDate(updatedDate);
        }
        catch (Exception e) {
            logger.error("Update user (id = {}) exception. Message: {}", user.getId(), e.getMessage());
            throw new DbOperationException("Update user exception");
        }
        if (rowsAffected < 1) {
            throw getAndLogUserNotFoundException(user.getId());
        }
        return user;
    }


    @Override
    public boolean delete(Long id) {
        int rowsAffected;
        try {
            rowsAffected = jdbcTemplate.update(Queries.SQL_SET_ACTIVE_STATUS_USER, false, id);
        }
        catch (Exception e) {
            logger.error("Delete user (id = {}) exception. Message: {}", id, e.getMessage());
            throw new DbOperationException("Delete user exception");
        }
        if (rowsAffected < 1) {
            throw getAndLogUserNotFoundException(id);
        }
        else {
            return true;
        }
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        int rowsAffected;

        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            rowsAffected = jdbcTemplate.update(Queries.SQL_UPDATE_USER_PASSWORD, encryptedPassword, userId);
        }
        catch (Exception e) {
            logger.error("Update user password (id = {}) exception. Message: {}", userId, e.getMessage());
            throw new DbOperationException("Update user password exception");
        }
        if (rowsAffected < 1) {
            throw getAndLogUserNotFoundException(userId);
        }
        else {
            return true;
        }
    }

    @Override
    public List<User> findUserByCommentId(Long commentId) {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_USERS_BY_COMMENT_ID, mapper, commentId);
        }
        catch (Exception e) {
            logger.error("Find users by comment id exception. Message: {}", e.getMessage());
            throw new DbOperationException("Find users by comment id exception");
        }
    }


    private NotFoundException getAndLogUserNotFoundException(Long id) {
        NotFoundException notFoundException = new NotFoundException("User not found");
        logger.error("Runtime exception. User not found (id = {}). Message: {}",
                id, notFoundException.getMessage());

        return notFoundException;
    }

    private PreparedStatement createStatement(User user, LocalDateTime currentDate, String encryptedPassword, Connection con) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                Queries.SQL_CREATE_USER, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, user.getEmail());
        ps.setString(2, encryptedPassword);
        ps.setString(3, user.getSurname());
        ps.setString(4, user.getName());
        ps.setString(5, user.getPhoneNumber());
        ps.setBoolean(6, user.isActive());
        ps.setObject(7, currentDate);
        ps.setObject(8, currentDate);

        return ps;
    }

    class Queries {

        static final String SQL_SET_ACTIVE_STATUS_USER = "UPDATE Users SET active = ? WHERE id = ?;";

        static final String SQL_UPDATE_USER = "UPDATE Users SET " +
                "email = ?, surname = ?," +
                " name = ?, phone_number = ?, active = ?, updated_date = ? WHERE id = ?;";

        static final String SQL_SELECT_USER_BY_ID = "SELECT * FROM Users WHERE id = ?";

        static final String SQL_SELECT_USER_BY_EMAIL = "SELECT * FROM Users WHERE email = ?";

        static final String SQL_SELECT_USERS_BY_ORGANIZATION_ID = "" +
                "SELECT * FROM Users WHERE id IN " +
                "(SELECT user_id FROM Users_organizations WHERE organization_id = ?)";

        static final String SQL_SELECT_USERS_BY_ROLES_IDS = "" +
                "SELECT * FROM Users WHERE id IN" +
                "(SELECT user_id FROM Users_roles WHERE role_id = ? )";

        static final String SQL_UPDATE_USER_PASSWORD = "UPDATE Users SET password = ? WHERE id = ?";

        static final String SQL_CREATE_USER = "" +
                "INSERT INTO Users(email, password, surname," +
                " name, phone_number, active, created_date, updated_date)" +
                " VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        static final String SQL_SELECT_ALL_USERS = "SELECT * FROM Users";

        static final String SQL_SELECT_USERS_BY_COMMENT_ID = "" +
                "SELECT * FROM Users WHERE id IN" +
                "(SELECT userId FROM SeenComments WHERE commentId = ? )";
    }

}

