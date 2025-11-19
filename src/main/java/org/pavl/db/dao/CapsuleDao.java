package org.pavl.db.dao;

import lombok.RequiredArgsConstructor;
import org.pavl.Utils;
import org.pavl.db.DbUtils;
import org.pavl.db.EntityUtils;
import org.pavl.db.domain.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.pavl.db.EntityUtils.*;

@RequiredArgsConstructor
public class CapsuleDao {

    private final DataSource dataSource;
    private final PrintWriter printWriter = new PrintWriter(System.out, true);

    public void addCapsule(CapsuleInput capsuleInput) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            UUID capsuleId = saveCapsule(connection, capsuleInput);
            saveTags(connection, capsuleInput.tags(), capsuleId);
            saveChainLink(connection, capsuleInput.chainId(), capsuleId);

            connection.commit();

        } catch (SQLException e) {
            printWriter.println("Database error occurred: " + e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                printWriter.println("Fatal error: can't do a rollback on transaction");
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    printWriter.println("Fatal error: can't close database connection");
                }
            }
        }
    }

    public CapsuleOutputDetails openCapsule(UUID capsuleId) {
        try (Connection connection = dataSource.getConnection()) {
            ResultSet capsuleSelectResultSet = getCapsuleResultSet(connection, capsuleId);
            if (!capsuleSelectResultSet.next()) {
                throw new IllegalArgumentException("Capsule with specified id doesn't exist");
            }

            if (!canBeOpened(capsuleSelectResultSet)) {
                throw new IllegalArgumentException("Capsule can't be opened at this time");
            }

            capsuleSelectResultSet.updateBoolean("is_opened", true);
            capsuleSelectResultSet.updateRow();
            capsuleSelectResultSet.previous();

            return convertToCapsuleOutputDetails(capsuleSelectResultSet);
        } catch (SQLException e) {
            printWriter.println("Database error occurred: " + e);
            return null;
        }
    }

    public List<CapsuleOutput> getCapsules(CapsuleFilter capsuleFilter) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement allCapsulesStatement = createCapsuleFilteringStatement(connection, capsuleFilter)
        ) {
            ResultSet resultSet = allCapsulesStatement.executeQuery();
            return convertToCapsuleOutput(resultSet);
        } catch (SQLException e) {
            printWriter.println("Database error occurred: " + e);
            return null;
        }
    }

    private UUID saveCapsule(Connection connection, CapsuleInput capsuleInput) throws SQLException {
        PreparedStatement saveCapsuleStatement = connection.prepareStatement(
                "insert into main.capsule(title, content, created_at, unlock_at, mood_score, is_opened, attachment_name, attachment)" +
                        " values (?, ?, current_date, ?, ?, ?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS
        );

        DbUtils.populateStatement(
                saveCapsuleStatement,
                capsuleInput.title(),
                Utils.encode(capsuleInput.content()),
                Date.valueOf(capsuleInput.unlockedAt()),
                capsuleInput.moodScore(),
                false
        );
        addAttachment(saveCapsuleStatement, capsuleInput.attachmentName(), capsuleInput.attachment());
        saveCapsuleStatement.executeUpdate();

        InputStream attachment = capsuleInput.attachment();
        try {
            if (attachment != null) {
                attachment.close();
            }
        } catch (IOException e) {
            printWriter.println("Error closing the attachment input stream");
        }

        return getGeneratedId(saveCapsuleStatement);
    }

    private void saveTags(Connection connection, Set<Tag> tags, UUID capsuleId) throws SQLException {
        if (tags.isEmpty()) {
            return;
        }

        PreparedStatement saveTagStatement = connection.prepareStatement(
                "insert into main.capsule_tag values (?, ?)"
        );

        for (Tag tag : tags) {
            DbUtils.populateStatement(saveTagStatement, capsuleId, tag.name().toLowerCase());
            saveTagStatement.addBatch();
        }
        saveTagStatement.executeBatch();
    }

    private void saveChainLink(Connection connection, UUID chainId, UUID capsuleId) throws SQLException {
        if (chainId == null) {
            return;
        }

        if (!chainExists(connection, chainId)) {
            throw new SQLException("Provided chain id is invalid");
        }

        ChainLinkInfo chainLinkInfo = getChainLinkInfo(connection, chainId);
        if (!chainLinkInfo.canCreate()) {
            throw new SQLException("Can't create new chain link, last capsule isn't opened yet");
        }

        PreparedStatement saveChainLinkStatement = connection.prepareStatement(
                "insert into main.chain_link values (?, ?, ?)"
        );
        DbUtils.populateStatement(saveChainLinkStatement, chainId, capsuleId, chainLinkInfo.lastLinkNumber() + 1);
        saveChainLinkStatement.executeUpdate();
    }

    private boolean chainExists(Connection connection, UUID chainId) throws SQLException {
        PreparedStatement chainSelectStatement = connection.prepareStatement(
                "select * from main.capsule_chain where id = ?"
        );
        chainSelectStatement.setObject(1, chainId);

        return chainSelectStatement.executeQuery().next();
    }

    private ResultSet getCapsuleResultSet(Connection connection, UUID capsuleId) throws SQLException {
        PreparedStatement capsuleSelectStatement = connection.prepareStatement(
                 """
                    select *
                    from main.capsule as c left outer join main.capsule_tag as ct on c.id = ct.capsule_id
                    where id = ?
                    """,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE
        );
        capsuleSelectStatement.setObject(1, capsuleId);

        return capsuleSelectStatement.executeQuery();
    }

    private boolean canBeOpened(ResultSet capsuleSelectResultSet) throws SQLException {
        LocalDate unlockAt = capsuleSelectResultSet.getObject("unlock_at", Date.class).toLocalDate();
        return LocalDate.now().isEqual(unlockAt) || LocalDate.now().isAfter(unlockAt);
    }

    private CapsuleOutputDetails convertToCapsuleOutputDetails(ResultSet capsuleSelectResultSet) throws SQLException {
        Map<UUID, CapsuleOutputDetails> capsuleOutputDetailsMap = new HashMap<>();
        while (capsuleSelectResultSet.next()) {
            UUID id = capsuleSelectResultSet.getObject(CAPSULE_ID_COLUMN, UUID.class);
            String title = capsuleSelectResultSet.getString(CAPSULE_TITLE_COLUMN);
            String content = Utils.decode(capsuleSelectResultSet.getString(CAPSULE_CONTENT_COLUMN));
            String attachmentName = capsuleSelectResultSet.getString(CAPSULE_ATTACHMENT_NAME_COLUMN);
            InputStream attachment = capsuleSelectResultSet.getBinaryStream(CAPSULE_ATTACHMENT_COLUMN);
            LocalDate createdAt = capsuleSelectResultSet.getDate(CAPSULE_CREATED_AT_COLUMN).toLocalDate();
            LocalDate unlockAt = capsuleSelectResultSet.getDate(CAPSULE_UNLOCK_AT_COLUMN).toLocalDate();
            Mood mood = Mood.fromScore(capsuleSelectResultSet.getInt(CAPSULE_MOOD_SCORE_COLUMN));
            boolean isOpen = capsuleSelectResultSet.getBoolean(CAPSULE_IS_OPENED_COLUMN);
            String tagAsString = capsuleSelectResultSet.getString(CAPSULE_TAG_COLUMN);

            Set<Tag> individualTag = Utils.createIndividualTag(tagAsString);

            capsuleOutputDetailsMap.compute(id, (k, v) -> {
                if (v == null) {
                    return new CapsuleOutputDetails(
                            id,
                            title,
                            content,
                            attachmentName,
                            attachment,
                            createdAt,
                            unlockAt,
                            individualTag,
                            mood,
                            isOpen
                    );
                } else {
                    Set<Tag> tags = v.getTags();
                    tags.addAll(individualTag);
                    return v;
                }
            });
        }
        return capsuleOutputDetailsMap.values().stream().findFirst().get();
    }

    private PreparedStatement createCapsuleFilteringStatement(Connection connection, CapsuleFilter capsuleFilter) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder("select * from main.capsule as c left outer join main.capsule_tag as ct on c.id = ct.capsule_id");
        if (capsuleFilter == null) {
            return connection.prepareStatement(queryBuilder.toString());
        }

        String query = createQuery(capsuleFilter, queryBuilder);

        List<Object> parameters = Stream.of(
                convertToOpenBoolean(capsuleFilter.capsuleState()),
                capsuleFilter.tags().stream().map(tag -> tag.name().toLowerCase()).collect(Collectors.toSet()),
                capsuleFilter.createdFrom(),
                capsuleFilter.createdTo(),
                capsuleFilter.unlockFrom(),
                capsuleFilter.unlockTo()
        ).filter(Objects::nonNull).toList();

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        DbUtils.populateStatement(preparedStatement, parameters.stream().flatMap(item -> {
            if (item instanceof Set<?>) {
                Set<Object> tags = (Set<Object>) item;
                return tags.stream();
            } else {
                return Stream.of(item);
            }
        }).toArray());
        return preparedStatement;
    }

    private List<CapsuleOutput> convertToCapsuleOutput(ResultSet resultSet) throws SQLException {
        Map<UUID, CapsuleOutput> capsuleOutputMap = new HashMap<>();

        while (resultSet.next()) {
            UUID id = resultSet.getObject(CAPSULE_ID_COLUMN, UUID.class);
            String title = resultSet.getString(CAPSULE_TITLE_COLUMN);
            LocalDate createdAt = resultSet.getObject(CAPSULE_CREATED_AT_COLUMN, LocalDate.class);
            LocalDate unlockAt = resultSet.getObject(CAPSULE_UNLOCK_AT_COLUMN, LocalDate.class);
            boolean isOpened = resultSet.getBoolean(CAPSULE_IS_OPENED_COLUMN);
            String tagAsString = resultSet.getString(CAPSULE_TAG_COLUMN);

            Set<Tag> individualTag = Utils.createIndividualTag(tagAsString);

            EntityUtils.handleNewValue(
                    capsuleOutputMap,
                    id,
                    title,
                    createdAt,
                    unlockAt,
                    individualTag,
                    isOpened
            );
        }

        return capsuleOutputMap.values().stream().toList();
    }

    private void addAttachment(PreparedStatement preparedStatement, String fileName, InputStream inputStream) throws SQLException {
        int attachmentNameColumnIndex = 6;
        int attachmentColumnIndex = 7;

        if (inputStream != null) {
            try {
                preparedStatement.setString(attachmentNameColumnIndex, fileName);
                preparedStatement.setBinaryStream(attachmentColumnIndex, inputStream, inputStream.available());
            } catch (IOException e) {
                printWriter.println("IO error occurred during attachment processing, skipping attachment saving");
            }
        } else {
            preparedStatement.setString(attachmentNameColumnIndex, null);
            preparedStatement.setObject(attachmentColumnIndex, null);
        }
    }

    private UUID getGeneratedId(PreparedStatement preparedStatement) throws SQLException {
        ResultSet result = preparedStatement.getGeneratedKeys();
        result.next();

        return result.getObject(1, UUID.class);
    }

    private ChainLinkInfo getChainLinkInfo(Connection connection, UUID chainId) throws SQLException {
        int lastLinkNumber = getLastLinkNumber(connection, chainId);
        if (lastLinkNumber == 0) {
            return new ChainLinkInfo(true, 0);
        }

        PreparedStatement chainLinkJoinStatement = connection.prepareStatement(
                """
                select is_opened
                from main.chain_link join main.capsule on capsule_id = id
                where chain_id = ? and sequence_order = ?
                """
        );
        DbUtils.populateStatement(chainLinkJoinStatement, chainId, lastLinkNumber);

        ResultSet resultSet = chainLinkJoinStatement.executeQuery();
        resultSet.next();
        boolean isOpened = resultSet.getBoolean(1);

        return new ChainLinkInfo(isOpened, lastLinkNumber);
    }

    private int getLastLinkNumber(Connection connection, UUID chainId) throws SQLException {
        PreparedStatement maxSequenceOrderStatement = connection.prepareStatement(
                """
                select max(sequence_order)
                from main.chain_link
                where chain_id = ?;
                """
        );
        DbUtils.populateStatement(maxSequenceOrderStatement, chainId);

        ResultSet resultSet = maxSequenceOrderStatement.executeQuery();
        if (!resultSet.next()) {
            return 0;
        }

        return resultSet.getInt(1);
    }

    private String createQuery(CapsuleFilter capsuleFilter, StringBuilder queryBuilder) {
        queryBuilder.append(" where ");
        List<String> filterPredicates = Stream.of(
                capsuleFilter.capsuleState() != null ? "is_opened = ?" : "",
                !capsuleFilter.tags().isEmpty() ? createTagQuery(capsuleFilter.tags()) : "",
                capsuleFilter.createdFrom() != null ? "created_at >= ?" : "",
                capsuleFilter.createdTo() != null ? "created_at <= ?" : "",
                capsuleFilter.unlockFrom() != null ? "unlock_at >= ?" : "",
                capsuleFilter.unlockTo() != null ? "unlock_at <= ?" : ""
        ).filter(predicate -> !predicate.isEmpty()).toList();



        queryBuilder.append(String.join(" and ", filterPredicates));
        return queryBuilder.toString();
    }

    private String createTagQuery(Set<Tag> tags) {
        int numberOfTags = tags.size();
        List<String> tagPatternList = Collections.nCopies(numberOfTags, "(?)");
        String tagPattern = String.join(", ", tagPatternList);

        return String.format("(not exists ((select * from (values %s)) except (select tag from main.capsule_tag where capsule_id = c.id)))", tagPattern);
    }

    private Boolean convertToOpenBoolean(CapsuleState capsuleState) {
        if (capsuleState == null) return null;
        return capsuleState.equals(CapsuleState.OPENED);
    }
}