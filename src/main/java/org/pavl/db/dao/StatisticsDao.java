package org.pavl.db.dao;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.pavl.Utils;
import org.pavl.cli.domain.StatFlag;
import org.pavl.db.domain.Tag;
import org.pavl.db.domain.stats.*;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
public class StatisticsDao {

    private final DataSource dataSource;
    private final PrintWriter printWriter = new PrintWriter(System.out, true);
    private final Map<StatFlag, Function<PreparedStatement, Object>> statFunctionMap = new EnumMap<StatFlag, Function<PreparedStatement, Object>>(Map.of(
            StatFlag.CAPSULE, StatisticsDao::getCapsuleStats,
            StatFlag.TIME, StatisticsDao::getTimeStats,
            StatFlag.MOOD, StatisticsDao::getMoodStats,
            StatFlag.TAG, StatisticsDao::getTagStats
    ));
    private final Map<StatFlag, String> statQueryMap = new EnumMap<>(Map.of(
            StatFlag.CAPSULE, """
                            select count(*)
                            from main.capsule;
                        
                            select count(*)
                            from main.capsule
                            where is_opened = true;
                        
                            select count(*)
                            from main.capsule
                            where unlock_at <= current_date;
                        
                            select count(*)
                            from main.capsule
                            where unlock_at > current_date;
                            """,
            StatFlag.TIME,  """
                            select min(created_at)
                            from main.capsule;
                        
                            select max(created_at)
                            from main.capsule;
                        
                            select created_at, unlock_at
                            from main.capsule;
                            """,
            StatFlag.MOOD,  """
                            select avg(mood_score), max(mood_score), min(mood_score)
                            from main.capsule;
                            """,
            StatFlag.TAG,   """
                            select count(distinct tag)
                            from main.capsule_tag;
                        
                            with frequencies as (
                                select tag, count(*) as count
                                from main.capsule_tag
                                group by tag
                            ),
                            max_row as (
                                select *
                                from frequencies
                                where count = (select max(count) from frequencies)
                                limit 1
                            ),
                            min_row as (
                                select *
                                from frequencies
                                where count = (select min(count) from frequencies)
                                limit 1
                            )
                            select tag, count
                            from ((select * from max_row) union all (select * from min_row)) as result
                            order by count desc;
                            """
    ));

    public Stats getStats(Set<StatFlag> statFlags) {
        try (Connection connection = dataSource.getConnection()) {
            List<String> queries = statQueryMap.keySet()
                    .stream()
                    .filter(statFlag -> statFlags.contains(statFlag) || statFlags.isEmpty())
                    .map(statQueryMap::get)
                    .toList();

            List<Function<PreparedStatement, Object>> functions = statFunctionMap.keySet()
                    .stream()
                    .map(statFlag -> statFlags.contains(statFlag) || statFlags.isEmpty() ? statFunctionMap.get(statFlag) : null)
                    .toList();

            String finalQuery = String.join("\n", queries);
            PreparedStatement preparedStatement = connection.prepareStatement(finalQuery);
            preparedStatement.execute();

            List<Object> results = functions
                    .stream()
                    .map(f -> Optional.ofNullable(f)
                            .map(func -> func.apply(preparedStatement))
                            .orElse(null)
                    )
                    .toList();

            return new Stats(
                    (CapsuleStats) results.get(0),
                    (TimeStats) results.get(1),
                    (MoodStats) results.get(2),
                    (TagStats) results.get(3)
            );
        } catch (SQLException e) {
            printWriter.println("Database error occurred: " + e);
            return null;
        }
    }

    @SneakyThrows
    private static Object getCapsuleStats(PreparedStatement preparedStatement) {
        int numQueries = 4;
        int[] resultValues = new int[numQueries];

        for (int i = 0; i < 4; i++) {
            ResultSet resultSet = preparedStatement.getResultSet();
            resultValues[i] = getIntFromResultSet(resultSet);
            preparedStatement.getMoreResults();
        }
        return new CapsuleStats(resultValues[0], resultValues[1], resultValues[2], resultValues[3]);
    }

    @SneakyThrows
    private static Object getTimeStats(PreparedStatement preparedStatement) {
        ResultSet minResultSet = preparedStatement.getResultSet();
        LocalDate oldestDate = getLocalDateFromResultSet(minResultSet);
        preparedStatement.getMoreResults();

        ResultSet maxResultSet = preparedStatement.getResultSet();
        LocalDate newestDate = getLocalDateFromResultSet(maxResultSet);

        preparedStatement.getMoreResults();
        ResultSet timeFrameResultSet = preparedStatement.getResultSet();

        List<TimeFrame> timeFrames = convertToTimeFrames(timeFrameResultSet);

        int averageWaitPeriodDays = getAverageWaitPeriodDays(timeFrames);
        LocalDate nextUnlock = Utils.upperBound(timeFrames.stream().map(TimeFrame::end).sorted().toList(), LocalDate.now());

        preparedStatement.getMoreResults();

        return new TimeStats(oldestDate, newestDate, nextUnlock, averageWaitPeriodDays);
    }

    @SneakyThrows
    private static Object getMoodStats(PreparedStatement preparedStatement) {
        ResultSet moodStatsResult = preparedStatement.getResultSet();
        MoodStats result = convertToMoodStats(moodStatsResult);
        preparedStatement.getMoreResults();
        return result;
    }

    @SneakyThrows
    private static Object getTagStats(PreparedStatement preparedStatement) {
        ResultSet uniqueTagCountResult = preparedStatement.getResultSet();
        uniqueTagCountResult.next();
        int uniqueTagCount = uniqueTagCountResult.getInt(1);
        preparedStatement.getMoreResults();

        ResultSet minMaxTagResultSet = preparedStatement.getResultSet();

        String maxTag;
        int maxCount;
        String minTag;
        int minCount;

        if (!minMaxTagResultSet.next()) {
            maxTag = null;
            maxCount = 0;
            minTag = null;
            minCount = 0;
        }else {
            maxTag = minMaxTagResultSet.getString(1);
            maxCount = minMaxTagResultSet.getInt(2);

            minMaxTagResultSet.next();
            minTag = minMaxTagResultSet.getString(1);
            minCount = minMaxTagResultSet.getInt(2);
        }

        Tag mostUsedTag;
        Tag leastUsedTag;

        try {
            mostUsedTag = Tag.fromName(maxTag);
            leastUsedTag = Tag.fromName(minTag);
        } catch (IllegalArgumentException ex) {
            mostUsedTag = null;
            leastUsedTag = null;
        }

        return new TagStats(
                uniqueTagCount,
                mostUsedTag,
                maxCount,
                leastUsedTag,
                minCount
        );
    }

    private static int getIntFromResultSet(ResultSet resultSet) throws SQLException {
        resultSet.next();
        return resultSet.getInt(1);
    }

    private static LocalDate getLocalDateFromResultSet(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }
        return Optional.ofNullable(resultSet.getDate(1))
                .map(Date::toLocalDate)
                .orElse(null);
    }

    private static List<TimeFrame> convertToTimeFrames(ResultSet timeFrameResultSet) throws SQLException {
        List<TimeFrame> result = new ArrayList<>();
        while (timeFrameResultSet.next()) {
            LocalDate createdAt = timeFrameResultSet.getDate("created_at").toLocalDate();
            LocalDate unlockAt = timeFrameResultSet.getDate("unlock_at").toLocalDate();
            result.add(new TimeFrame(createdAt, unlockAt));
        }
        return result;
    }

    private static int getAverageWaitPeriodDays(List<TimeFrame> timeFrames) {
        return (int) timeFrames
                .stream()
                .map(timeFrame -> Period.between(timeFrame.start(), timeFrame.end()))
                .map(Period::getDays)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
    }

    private static MoodStats convertToMoodStats(ResultSet resultSet) throws SQLException {
        resultSet.next();
        return new MoodStats(resultSet.getDouble(1), resultSet.getInt(2), resultSet.getInt(3));
    }
}
