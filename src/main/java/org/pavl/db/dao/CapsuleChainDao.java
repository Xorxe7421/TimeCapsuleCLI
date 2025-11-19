package org.pavl.db.dao;

import lombok.RequiredArgsConstructor;
import org.pavl.Utils;
import org.pavl.db.DbUtils;
import org.pavl.db.EntityUtils;
import org.pavl.db.domain.CapsuleChainOutput;
import org.pavl.db.domain.CapsuleOutput;
import org.pavl.db.domain.MappedCapsuleChainOutput;
import org.pavl.db.domain.Tag;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.pavl.db.EntityUtils.*;

@RequiredArgsConstructor
public class CapsuleChainDao {

    private final DataSource dataSource;
    private final PrintWriter printWriter = new PrintWriter(System.out, true);

    public void addCapsuleChain(String name) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "insert into main.capsule_chain (name) values (?)"
             )
        ) {
            DbUtils.populateStatement(preparedStatement, name);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            printWriter.println("Database error occurred: " + e);
        }
    }

    public List<CapsuleChainOutput> getAllCapsuleChains() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     """
                       select ch.id as chain_id,
                              ch.name as chain_name,
                              c.id as capsule_id,
                              c.title as title,
                              c.created_at as created_at,
                              c.unlock_at as unlock_at,
                              ct.tag as tag,
                              c.is_opened as is_opened
                       from main.capsule_chain as ch
                           left outer join main.chain_link as cl on ch.id = cl.chain_id
                           left outer join main.capsule as c on cl.capsule_id = c.id
                           left outer join main.capsule_tag as ct on c.id = ct.capsule_id
                       """
             )
        ) {
            ResultSet resultSet = preparedStatement.executeQuery();
            return convertToCapsuleChainOutput(resultSet);
        } catch (SQLException e) {
            printWriter.println("Database error occurred: " + e);
            return null;
        }
    }

    private List<CapsuleChainOutput> convertToCapsuleChainOutput(ResultSet resultSet) throws SQLException {
        Map<UUID, MappedCapsuleChainOutput> capsuleChainOutputMap = new HashMap<>();

        while (resultSet.next()) {
            UUID chainId = resultSet.getObject(CHAIN_ID_COLUMN, UUID.class);
            String chainName = resultSet.getString(CHAIN_NAME_COLUMN);
            UUID capsuleId = resultSet.getObject(CHAIN_CAPSULE_ID_COLUMN, UUID.class);
            String capsuleTitle = resultSet.getString(CAPSULE_TITLE_COLUMN);
            LocalDate capsuleCreatedAt = Optional.ofNullable(resultSet.getDate(CAPSULE_CREATED_AT_COLUMN)).map(Date::toLocalDate).orElse(null);
            LocalDate capsuleUnlockAt = Optional.ofNullable(resultSet.getDate(CAPSULE_UNLOCK_AT_COLUMN)).map(Date::toLocalDate).orElse(null);
            String tagAsString = resultSet.getString(CAPSULE_TAG_COLUMN);
            boolean capsuleIsOpen = resultSet.getBoolean(CAPSULE_IS_OPENED_COLUMN);

            Set<Tag> individualTag = Utils.createIndividualTag(tagAsString);

            capsuleChainOutputMap.compute(chainId, (k, v) -> {
                if (v == null) {
                    if (capsuleId == null) {
                        return new MappedCapsuleChainOutput(
                                chainId,
                                chainName,
                                Collections.emptyMap()
                        );
                    }

                    return new MappedCapsuleChainOutput(
                            chainId,
                            chainName,
                            new HashMap<>(
                                Map.of(capsuleId,
                                        new CapsuleOutput(
                                                capsuleId,
                                                capsuleTitle,
                                                capsuleCreatedAt,
                                                capsuleUnlockAt,
                                                individualTag,
                                                capsuleIsOpen
                                        )
                                )
                            )
                    );
                } else {
                    Map<UUID, CapsuleOutput> capsuleOutputMap = v.capsuleMap();
                    EntityUtils.handleNewValue(
                            capsuleOutputMap,
                            capsuleId,
                            capsuleTitle,
                            capsuleCreatedAt,
                            capsuleUnlockAt,
                            individualTag,
                            capsuleIsOpen
                    );
                    return new MappedCapsuleChainOutput(
                            chainId,
                            chainName,
                            capsuleOutputMap
                    );
                }
            });
        }

        return capsuleChainOutputMap.values()
                .stream()
                .map(chain ->
                        new CapsuleChainOutput(
                                chain.id(),
                                chain.name(),
                                chain.capsuleMap().values()
                                        .stream()
                                        .toList()
                        )
                )
                .toList();
    }
}
