package org.pavl.cli;

import org.pavl.Utils;
import org.pavl.cli.domain.*;
import org.pavl.db.dao.CapsuleChainDao;
import org.pavl.db.dao.CapsuleDao;
import org.pavl.db.dao.StatisticsDao;
import org.pavl.db.domain.*;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CliHandler {

    private final CapsuleDao capsuleDao;
    private final CapsuleChainDao capsuleChainDao;
    private final StatisticsDao statisticsDao;

    private static final int CAPSULE_CREATION_POSITIONAL_ARGUMENT_SIZE = 5;
    private static final int CHAIN_CREATION_POSITIONAL_ARGUMENT_SIZE = 2;
    private static final int LISTING_POSITIONAL_ARGUMENT_SIZE = 1;
    private static final int OPEN_POSITIONAL_ARGUMENT_SIZE = 1;

    private final PrintWriter printWriter = new PrintWriter(System.out, true);

    public CliHandler() {
        Properties properties = getDatabaseProperties();
        DataSource dataSource = createDataSource(properties);

        capsuleDao = new CapsuleDao(dataSource);
        capsuleChainDao = new CapsuleChainDao(dataSource);
        statisticsDao = new StatisticsDao(dataSource);
    }

    private Properties getDatabaseProperties() {
        Properties properties = new Properties();
        try (InputStream resourceStream =
                     ClassLoader.getSystemResourceAsStream("datasource.properties")) {
            properties.load(resourceStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private DataSource createDataSource(Properties properties) {
        String url = properties.getProperty("database.url");
        int port = Integer.parseInt(properties.getProperty("database.port"));
        String user = System.getenv("database_user");
        String password = System.getenv("database_password");
        String name = properties.getProperty("database.name");

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[] {url});
        dataSource.setPortNumbers(new int[] {port});
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setDatabaseName(name);

        return dataSource;
    }

    public void handle(String[] args) {
        CliInput cliInput = createCliInput(args);
        if (cliInput == null) {
            return;
        }

        switch (cliInput.command()) {
            case CREATE -> handleCreateCommand(cliInput.positionalArguments(), cliInput.flags());
            case LIST -> handleListCommand(cliInput.positionalArguments(), cliInput.flags());
            case OPEN -> handleOpenCommand(cliInput.positionalArguments(), cliInput.flags());
            case STATS -> handleStatsCommand(cliInput.positionalArguments(), cliInput.flags());
        }
    }

    private CliInput createCliInput(String[] args) {
        try {
            return convertToCliInput(args);
        } catch (IllegalArgumentException ex) {
            printWriter.println(ex.getMessage());
            return null;
        }
    }

    private void handleCreateCommand(List<String> positionalArguments, List<String> flags) {
        Entity entity = getEntity(positionalArguments);
        if (entity == null) {
            return;
        }

        if (entity == Entity.CAPSULE) {
            handleCapsuleCreation(positionalArguments, flags);
        } else {
            handleChainCreation(positionalArguments, flags);
        }
    }

    private void handleListCommand(List<String> positionalArguments, List<String> flags) {
        Entity entity = getEntity(positionalArguments);
        if (entity == null) {
            return;
        }

        if (positionalArguments.size() != LISTING_POSITIONAL_ARGUMENT_SIZE) {
            printWriter.println("Wrong number of positional arguments");
            return;
        }

        if (entity == Entity.CAPSULE) {
            Map<CapsuleListingFlag, String> capsuleListingFlags = getFlagValueMap(flags, CapsuleListingFlag.class, CapsuleListingFlag::fromName);
            if (capsuleListingFlags == null) {
                return;
            }

            if (capsuleListingFlags.isEmpty()) {
                printWriter.println(Utils.toString(capsuleDao.getCapsules(null), 1));
                return;
            }

            CapsuleFilter capsuleFilter = createCapsuleFilter(capsuleListingFlags);
            if (capsuleFilter == null) {
                return;
            }

            printWriter.println(Utils.toString(capsuleDao.getCapsules(capsuleFilter), 1));
        } else {
            if (!flags.isEmpty()) {
                printWriter.println("Flags aren't allowed here");
                return;
            }

            printWriter.println(Utils.toString(capsuleChainDao.getAllCapsuleChains(), 3));
        }
    }

    private void handleOpenCommand(List<String> positionalArguments, List<String> flags) {
        if (!flags.isEmpty()) {
            printWriter.println("Flags aren't allowed here");
            return;
        }

        if (positionalArguments.size() != OPEN_POSITIONAL_ARGUMENT_SIZE) {
            printWriter.println("Wrong number of positional arguments");
            return;
        }

        UUID capsuleId;
        try {
            capsuleId = UUID.fromString(positionalArguments.getFirst());
        } catch (IllegalArgumentException ex) {
            printWriter.println("Invalid id");
            return;
        }

        CapsuleOutputDetails capsuleOutputDetails;
        try {
            capsuleOutputDetails = capsuleDao.openCapsule(capsuleId);
        } catch (IllegalArgumentException ex) {
            printWriter.println(ex.getMessage());
            return;
        }

        if (capsuleOutputDetails != null) {
            if (capsuleOutputDetails.getAttachment() != null) {
                try {
                    Files.write(Path.of(capsuleOutputDetails.getAttachmentName()), capsuleOutputDetails.getAttachment().readAllBytes());
                } catch (IOException ex) {
                    printWriter.println("Error during file creation");
                }
            }
            printWriter.println(capsuleOutputDetails);
        }
    }

    private void handleStatsCommand(List<String> positionalArguments, List<String> flags) {
        if (!positionalArguments.isEmpty()) {
            printWriter.println("Positional arguments are illegal here");
            return;
        }

        if (flags.isEmpty()) {
            printWriter.println(statisticsDao.getStats(Collections.emptySet()));
            return;
        }

        Set<StatFlag> statFlags;
        try {
            statFlags = EnumSet.copyOf(
                    flags
                            .stream()
                            .map(StatFlag::fromName)
                            .collect(Collectors.toSet())
            );
        } catch (IllegalArgumentException ex) {
            printWriter.println(ex.getMessage());
            return;
        }

        printWriter.println(statisticsDao.getStats(statFlags));
    }

    private CapsuleFilter createCapsuleFilter(Map<CapsuleListingFlag, String> capsuleListingFlags) {
        CapsuleState capsuleState;
        try {
            capsuleState = Optional.ofNullable(capsuleListingFlags.get(CapsuleListingFlag.CAPSULE_STATE))
                    .map(flag -> CapsuleState.valueOf(flag.toUpperCase()))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            printWriter.println("invalid capsule state");
            return null;
        }

        String tagsAsString = capsuleListingFlags.getOrDefault(CapsuleListingFlag.TAGS, null);
        Set<Tag> tags = getTags(tagsAsString);
        if (tags == null) {
            return null;
        }

        LocalDate createdFrom;
        LocalDate createdTo;
        LocalDate unlockFrom;
        LocalDate unlockTo;

        try {
            createdFrom = getLocalDate(capsuleListingFlags, CapsuleListingFlag.CREATED_FROM);
            createdTo = getLocalDate(capsuleListingFlags, CapsuleListingFlag.CREATED_TO);
            unlockFrom = getLocalDate(capsuleListingFlags, CapsuleListingFlag.UNLOCK_FROM);
            unlockTo = getLocalDate(capsuleListingFlags, CapsuleListingFlag.UNLOCK_TO);
        } catch (DateTimeParseException ex) {
            printWriter.println("Invalid date format");
            return null;
        }

        return new CapsuleFilter(capsuleState, tags, createdFrom, createdTo, unlockFrom, unlockTo);
    }

    private LocalDate getLocalDate(Map<CapsuleListingFlag, String> capsuleListingFlags, CapsuleListingFlag capsuleListingFlag) {
        return Optional.ofNullable(
                        capsuleListingFlags.getOrDefault(
                                capsuleListingFlag,
                                null
                        ))
                .map(LocalDate::parse)
                .orElse(null);
    }

    private Entity getEntity(List<String> positionalArguments) {
        try {
            return Entity.fromName(positionalArguments.getFirst());
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            printWriter.println("Valid entity isn't provided");
            return null;
        }
    }

    private void handleCapsuleCreation(List<String> positionalArguments, List<String> flags) {
        if (positionalArguments.size() != CAPSULE_CREATION_POSITIONAL_ARGUMENT_SIZE) {
            printWriter.println("Wrong number of positional arguments");
            return;
        }

        String title = positionalArguments.get(1);
        String content = positionalArguments.get(2);

        LocalDate unlockedAt = getCreatedAt(positionalArguments);
        if (unlockedAt == null) {
            return;
        }

        Integer moodScore = getMoodScore(positionalArguments);
        if (moodScore == null) {
            return;
        }

        Map<CapsuleCreationFlag, String> flagValueMap = getFlagValueMap(flags, CapsuleCreationFlag.class, CapsuleCreationFlag::fromName);
        if (flagValueMap == null) {
            return;
        }

        String attachment = flagValueMap.getOrDefault(CapsuleCreationFlag.ATTACHMENT, null);
        InputStream attachmentInputStream;
        String attachmentFileName;

        if (attachment == null) {
            attachmentInputStream = null;
            attachmentFileName = null;
        } else {
            Path attachmentFile;
            try {
                attachmentFile = Path.of(attachment);
            } catch (InvalidPathException ex) {
                printWriter.println("Invalid file path");
                return;
            }
            attachmentFileName = attachmentFile.getFileName().toString();

            try {
                attachmentInputStream = Files.newInputStream(attachmentFile);
            } catch (IOException e) {
                printWriter.println(e);
                return;
            }
        }

        Set<Tag> tags = getTags(flagValueMap);
        if (tags == null) {
            return;
        }

        String chainIdAsString = flagValueMap.getOrDefault(CapsuleCreationFlag.CHAIN_ID, null);
        UUID chainId;
        try {
            chainId = chainIdAsString != null ? UUID.fromString(chainIdAsString) : null;
        } catch (IllegalArgumentException ex) {
            printWriter.println("Invalid chainId parameter");
            return;
        }

        capsuleDao.addCapsule(
                new CapsuleInput(
                    title,
                    content,
                    attachmentFileName,
                    attachmentInputStream,
                    unlockedAt,
                    moodScore,
                    tags,
                    chainId
            )
        );
    }

    private Set<Tag> getTags(Map<CapsuleCreationFlag, String> flagValueMap) {
        String tagsAsString = flagValueMap.getOrDefault(CapsuleCreationFlag.TAGS, null);
        return getTags(tagsAsString);
    }

    private Set<Tag> getTags(String tagsAsString) {
        try {
            return Optional.ofNullable(tagsAsString)
                    .map(this::convertToTags)
                    .orElse(EnumSet.noneOf(Tag.class));
        } catch (IllegalArgumentException ex) {
            printWriter.println("Invalid tags");
            return null;
        }
    }

    private <T extends Enum<T>> Map<T, String> getFlagValueMap(List<String> flags, Class<T> enumType, Function<String, T> stringToEnumFunc) {
        if (flags.size() > getEnumCount(enumType)) {
            printWriter.println("Wrong number of flags");
            return null;
        }

        try {
            return createFlagValueMap(flags, stringToEnumFunc);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printWriter.println("Invalid flags");
            return null;
        }
    }

    private Integer getMoodScore(List<String> positionalArguments) {
        try {
            return Integer.parseInt(positionalArguments.get(4));
        } catch (NumberFormatException ex) {
            printWriter.println("Invalid moodScore parameter");
            return null;
        }
    }

    private LocalDate getCreatedAt(List<String> positionalArguments) {
        LocalDate unlockedAt;
        try {
            unlockedAt = LocalDate.parse(positionalArguments.get(3));
        } catch (DateTimeParseException ex) {
            printWriter.println("Invalid unlockAt parameter");
            return null;
        }

        if (unlockedAt.isBefore(LocalDate.now())) {
            printWriter.println("UnlockAt is in the past");
            return null;
        }
        return unlockedAt;
    }

    private void handleChainCreation(List<String> positionalArguments, List<String> flags) {
        if (!flags.isEmpty()) {
            printWriter.println("Flags aren't allowed here");
            return;
        }

        if (positionalArguments.size() != CHAIN_CREATION_POSITIONAL_ARGUMENT_SIZE) {
            printWriter.println("Wrong number of positional arguments");
            return;
        }

        String chainName = positionalArguments.get(1);
        capsuleChainDao.addCapsuleChain(chainName);
    }

    private CliInput convertToCliInput(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Command not provided");
        }

        Command command = Command.fromName(args[0]);
        List<String> positionalArguments = new ArrayList<>();

        int index = 1;
        while (index < args.length) {
            String arg = args[index];
            if (arg.startsWith("--")) {
                break;
            }
            positionalArguments.add(arg);
            index++;
        }

        List<String> flags = Arrays.asList(args)
                .subList(index, args.length)
                .stream()
                .map(flag -> flag.substring(2))
                .toList();

        return new CliInput(command, positionalArguments, flags);
    }

    private <T extends Enum<T>> Map<T, String> createFlagValueMap(
            List<String> flags,
            Function<String, T> stringToEnumFunc
    ) {
        if (flags.isEmpty()) {
            return Collections.emptyMap();
        }

        return new EnumMap<T, String>(
                flags
                .stream()
                .map(flag -> {
                    String[] split = flag.split("=");
                    if (split.length != 2) {
                        throw new IllegalArgumentException("Invalid flag: " + flag);
                    }

                    T enumFlag = stringToEnumFunc.apply(split[0]);

                    Object[] keyValue = new Object[2];
                    keyValue[0] = enumFlag;
                    keyValue[1] = split[1];
                    return keyValue;
                })
                .collect(Collectors.toMap(
                    item -> (T) item[0],
                        item -> (String) item[1]
                ))
        );
    }

    private Set<Tag> convertToTags(String tagsAsString) {
        String[] tags = tagsAsString.split(",");
        Tag[] tagArray = Arrays.stream(tags).map(Tag::fromName).toArray(Tag[]::new);
        return EnumSet.copyOf(Arrays.asList(tagArray));
    }

    private <T extends Enum<T>> int getEnumCount(Class<T> enumType) {
        return enumType.getEnumConstants().length;
    }
}
