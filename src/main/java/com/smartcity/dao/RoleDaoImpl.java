package com.smartcity.dao;

import com.smartcity.domain.Role;
import com.smartcity.exceptions.DbOperationException;
import com.smartcity.exceptions.NotFoundException;
import com.smartcity.mapper.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleDaoImpl implements RoleDao {

    private static final Logger logger = LoggerFactory.getLogger(RoleDaoImpl.class);

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public RoleDaoImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Role create(Role role) {
        try {
            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            LocalDateTime time = LocalDateTime.now();
            role.setCreatedDate(time);
            role.setUpdatedDate(time);

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(Queries.SQL_ROLE_CREATE, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, role.getName());
                ps.setObject(2, role.getCreatedDate());
                ps.setObject(3, role.getUpdatedDate());
                return ps;
            }, holder);
            role.setId(Optional.ofNullable(holder.getKey().longValue())
                    .orElseThrow(() -> new DbOperationException("Create Role Dao method error: AutoGeneratedKey = null")));
            return role;
        } catch (Exception e) {
            logger.error("Can't create role{}. Error {}", role, e.getMessage());
            throw new DbOperationException("Can't create role by id = " + role.getId() + " Role: " + role);
        }
    }

    public Role get(Long id) {

        try {
            Role role = jdbcTemplate.queryForObject(Queries.SQL_ROLE_GET_BY_ID, RoleMapper.getInstance(), id);
            return role;
        } catch (EmptyResultDataAccessException | NotFoundException e) {
            throw getAndLogRoleNotFoundException(id);
        } catch (Exception e) {
            logger.error("Can't get role with id={}. Error: ", id, e);
            throw new DbOperationException("Can't get role with id = " + id);
        }
    }


    public List<Role> getRolesByUserId(Long id){
        try {
            return this.jdbcTemplate.query(Queries.SQL_GET_ROLES_BY_USER_ID, RoleMapper.getInstance(), id);
        }
        catch (Exception e){
            logger.error("Can't get Roles by User_id = {}. Error: ",id, e);
            throw new DbOperationException("Can't get roles by Users_id = id "+id);
        }
    }

    public List<Role> getAll() {
        try {
            return this.jdbcTemplate.query(Queries.SQL_ROLE_GET_ALL, RoleMapper.getInstance());
        } catch (Exception e) {
            logger.error("Can't get all Roles. Error: ", e.getMessage());
            throw new DbOperationException("Can't get all Roles");
        }
    }


    public Role update(Role role) {
        Role roleFromDB;

        try {
            roleFromDB = this.get(role.getId());
        } catch (NotFoundException e) {
            throw getAndLogRoleNotFoundException(role.getId());
        }

        try {
            role.setCreatedDate(roleFromDB.getCreatedDate());
            role.setUpdatedDate(LocalDateTime.now());

            jdbcTemplate.update(Queries.SQL_ROLE_UPDATE,
                    role.getName(),
                    role.getUpdatedDate(),
                    role.getId());
            return role;
        } catch (Exception e) {
            logger.error("Update role error: " + role + " " + e.getMessage());
            throw new DbOperationException("Update role error");
        }
    }



    public boolean delete(Long id) {

        try {
            this.get(id);
        } catch (NotFoundException e) {
            throw getAndLogRoleNotFoundException(id);
        }

        try {
            jdbcTemplate.update(Queries.SQL_ROLE_DELETE, id);
            return true;
        } catch (Exception e) {
            logger.error("Delete role error. " + e.getMessage());
            throw new DbOperationException("Delete Role exception");
        }
    }

    private NotFoundException getAndLogRoleNotFoundException(Long id) {
        NotFoundException notFoundException = new NotFoundException("Role not found");
        logger.error("Runtime exception. Role not found id = {}. Message: {}", id, notFoundException.getMessage());
        return notFoundException;
    }



    class Queries {

        static final String SQL_ROLE_DELETE = "delete from    Roles where id = ?";

        static final String SQL_ROLE_CREATE = "insert into Roles (name,created_date,updated_date) values (?,?,?)";

        static final String SQL_ROLE_UPDATE = "update Roles set name = ?, updated_date = ? where id = ?";

        static final String SQL_ROLE_GET_BY_ID = "select * from Roles where id = ?";

        static final String SQL_ROLE_GET_ALL = "select * from Roles";

        static final String SQL_GET_ROLES_BY_USER_ID = "SELECT * FROM Roles JOIN Users_roles ON Roles.id = Users_roles.role_id WHERE Users_roles.user_id = ?";
    }
}
