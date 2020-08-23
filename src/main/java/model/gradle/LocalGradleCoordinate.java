package model.gradle;

import com.google.common.base.Joiner;
import constants.FileConstants;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalGradleCoordinate implements GradleCoordinate, Comparable<LocalGradleCoordinate> {
    private static final String NONE = "NONE";
    private boolean isUsed = false;
    public static final String PREVIEW_ID = "rc";
    public static final PlusComponent PLUS_REV = new LocalGradleCoordinate.PlusComponent();
    public static final int PLUS_REV_VALUE = -1;
    private final String mGroupId;
    private final String mArtifactId;
    private int versionOrder = -1;
    private final ArtifactType mArtifactType;
    private final List<RevisionComponent> mRevisions;
    private static final Pattern MAVEN_PATTERN = Pattern.compile("([\\w\\d\\.-]+):([\\w\\d\\.-]+):([^:@]+)(@[\\w-]+)?");
    public static final Comparator<LocalGradleCoordinate> COMPARE_PLUS_LOWER = new LocalGradleCoordinate.GradleCoordinateComparator(-1);
    public static final Comparator<LocalGradleCoordinate> COMPARE_PLUS_HIGHER = new LocalGradleCoordinate.GradleCoordinateComparator(1);

    public LocalGradleCoordinate(String groupId, String artifactId, RevisionComponent... revisions) {
        this(groupId, artifactId, Arrays.asList(revisions), null);
    }


    public LocalGradleCoordinate(String groupId, String artifactId, String revision) {
        this(groupId, artifactId, parseRevisionNumber(revision), null);
    }

    public LocalGradleCoordinate(String groupId, String artifactId, String revision, int versionOrder) {
        this(groupId, artifactId, parseRevisionNumber(revision), null);
        this.versionOrder = versionOrder;
    }

    public LocalGradleCoordinate(String groupId, String artifactId, int... revisions) {
        this(groupId, artifactId, createComponents(revisions), null);
    }

    public void setIsUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }

    public boolean isUsed() {
        return isUsed;
    }

    private static List<RevisionComponent> createComponents(int[] revisions) {
        List<RevisionComponent> result = new ArrayList(revisions.length);
        int[] var2 = revisions;
        int var3 = revisions.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            int revision = var2[var4];
            if (revision == -1) {
                result.add(PLUS_REV);
            } else {
                result.add(new NumberComponent(revision));
            }
        }

        return result;
    }

    public LocalGradleCoordinate(String groupId, String artifactId, List<RevisionComponent> revisions, ArtifactType type) {
        this.mRevisions = new ArrayList(3);
        this.mGroupId = groupId;
        this.mArtifactId = artifactId;
        this.mRevisions.addAll(revisions);
        this.mArtifactType = type;
    }

    public static LocalGradleCoordinate parseCoordinateString(String coordinateString) {
        if (coordinateString == null) {
            return null;
        } else {
            Matcher matcher = MAVEN_PATTERN.matcher(coordinateString);
            if (!matcher.matches()) {
                return null;
            } else {
                String groupId = matcher.group(1);
                String artifactId = matcher.group(2);
                String revision = matcher.group(3);
                String typeString = matcher.group(4);
                ArtifactType type = null;
                if (typeString != null) {
                    type = ArtifactType.getArtifactType(typeString.substring(1));
                }

                List<RevisionComponent> revisions = parseRevisionNumber(revision);
                return new LocalGradleCoordinate(groupId, artifactId, revisions, type);
            }
        }
    }

    public static LocalGradleCoordinate parseVersionOnly(String revision) {
        return new LocalGradleCoordinate("NONE", "NONE", parseRevisionNumber(revision), null);
    }

    public static List<RevisionComponent> parseRevisionNumber(String revision) {
        List<RevisionComponent> components = new ArrayList();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < revision.length(); ++i) {
            char c = revision.charAt(i);
            if (c == '.') {
                flushBuffer(components, buffer, true);
            } else {
                if (c == '+') {
                    if (buffer.length() > 0) {
                        flushBuffer(components, buffer, true);
                    }

                    components.add(PLUS_REV);
                    break;
                }

                if (c == '-') {
                    flushBuffer(components, buffer, false);
                    int last = components.size() - 1;
                    if (last == -1) {
                        components.add(ListComponent.of(new NumberComponent(0)));
                    } else if (!(components.get(last) instanceof ListComponent)) {
                        components.set(last, ListComponent.of((RevisionComponent) components.get(last)));
                    }
                } else {
                    buffer.append(c);
                }
            }
        }

        if (buffer.length() > 0 || components.isEmpty()) {
            flushBuffer(components, buffer, true);
        }

        return components;
    }

    private static void flushBuffer(List<RevisionComponent> components, StringBuilder buffer, boolean closeList) {
        RevisionComponent newComponent;
        if (buffer.length() == 0) {
            newComponent = new NumberComponent(0);
        } else {
            String string = buffer.toString();

            try {
                int number = Integer.parseInt(string);
                if (string.length() > 1 && string.charAt(0) == '0') {
                    newComponent = new PaddedNumberComponent(number, string);
                } else {
                    newComponent = new NumberComponent(number);
                }
            } catch (NumberFormatException var6) {
                newComponent = new StringComponent(string);
            }
        }

        buffer.setLength(0);
        if (!components.isEmpty() && components.get(components.size() - 1) instanceof ListComponent) {
            ListComponent component = (ListComponent) components.get(components.size() - 1);
            if (!component.mClosed) {
                component.add(newComponent);
                if (closeList) {
                    component.mClosed = true;
                }

                return;
            }
        }

        components.add(newComponent);
    }

    public String toString() {
        String s = String.format(Locale.US, "%s:%s:%s", this.mGroupId, this.mArtifactId, this.getRevision());
        if (this.mArtifactType != null) {
            s = s + "@" + this.mArtifactType.toString();
        }

        return s;
    }

    public String getName() {
        return this.mGroupId + FileConstants.NEW_GROUP_ARTIFACT_SEPARATOR + this.mArtifactId;
    }


    public String getGroupId() {
        return this.mGroupId;
    }

    public String getArtifactId() {
        return this.mArtifactId;
    }

    public ArtifactType getArtifactType() {
        return this.mArtifactType;
    }

    public String getId() {
        return this.mGroupId != null && this.mArtifactId != null ? String.format("%s:%s", this.mGroupId, this.mArtifactId) : null;
    }

    public ArtifactType getType() {
        return this.mArtifactType;
    }

    public boolean acceptsGreaterRevisions() {
        return this.mRevisions.get(this.mRevisions.size() - 1) == PLUS_REV;
    }

    public String getRevision() {
        StringBuilder revision = new StringBuilder();

        RevisionComponent component;
        for (Iterator var2 = this.mRevisions.iterator(); var2.hasNext(); revision.append(component.toString())) {
            component = (RevisionComponent) var2.next();
            if (revision.length() > 0) {
                revision.append('.');
            }
        }

        return revision.toString();
    }

    public boolean isPreview() {
        return !this.mRevisions.isEmpty() && this.mRevisions.get(this.mRevisions.size() - 1).isPreview();
    }

    public int getMajorVersion() {
        return this.mRevisions.isEmpty() ? -2147483648 : this.mRevisions.get(0).asInteger();
    }

    public int getMinorVersion() {
        return this.mRevisions.size() < 2 ? -2147483648 : this.mRevisions.get(1).asInteger();
    }

    public int getMicroVersion() {
        return this.mRevisions.size() < 3 ? -2147483648 : this.mRevisions.get(2).asInteger();
    }

    public boolean isSameArtifact(LocalGradleCoordinate o) {
        return o.mGroupId.equals(this.mGroupId) && o.mArtifactId.equals(this.mArtifactId);
    }

    public boolean matches(LocalGradleCoordinate pattern) {
        if (!this.isSameArtifact(pattern)) {
            return false;
        } else {
            Iterator<RevisionComponent> thisRev = this.mRevisions.iterator();
            Iterator var3 = pattern.mRevisions.iterator();

            RevisionComponent thatRev;
            do {
                if (!var3.hasNext()) {
                    do {
                        if (!thisRev.hasNext()) {
                            return true;
                        }
                    } while (((RevisionComponent) thisRev.next()).asInteger() == 0);

                    return false;
                }

                thatRev = (RevisionComponent) var3.next();
                if (thatRev instanceof PlusComponent) {
                    return true;
                }

                if (!thisRev.hasNext() && thatRev.asInteger() != 0) {
                    return false;
                }
            } while (!thisRev.hasNext() || thatRev.equals(thisRev.next()));

            return false;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            LocalGradleCoordinate that = (LocalGradleCoordinate) o;
            if (!this.mRevisions.equals(that.mRevisions)) {
                return false;
            } else if (!this.mArtifactId.equals(that.mArtifactId)) {
                return false;
            } else if (!this.mGroupId.equals(that.mGroupId)) {
                return false;
            } else if (this.mArtifactType == null != (that.mArtifactType == null)) {
                return false;
            } else {
                return this.mArtifactType == null || this.mArtifactType.equals(that.mArtifactType);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.mGroupId.hashCode();
        result = 31 * result + this.mArtifactId.hashCode();

        RevisionComponent component;
        for (Iterator var2 = this.mRevisions.iterator(); var2.hasNext(); result = 31 * result + component.hashCode()) {
            component = (RevisionComponent) var2.next();
        }

        if (this.mArtifactType != null) {
            result = 31 * result + this.mArtifactType.hashCode();
        }

        return result;
    }

    public int getVersionOrder() {
        return versionOrder;
    }

    @Override
    public int compareTo(@NotNull LocalGradleCoordinate o) {
        return this.getVersionOrder() - o.getVersionOrder();
    }


    private static class GradleCoordinateComparator implements Comparator<LocalGradleCoordinate> {
        private final int mPlusResult;

        private GradleCoordinateComparator(int plusResult) {
            this.mPlusResult = plusResult;
        }

        public int compare(LocalGradleCoordinate a, LocalGradleCoordinate b) {
            if (!a.isSameArtifact(b)) {
                return a.mArtifactId.compareTo(b.mArtifactId);
            } else {
                return a.getVersionOrder() - b.getVersionOrder();
            }
        }
    }

    public static class ListComponent extends RevisionComponent {
        private final List<RevisionComponent> mItems = new ArrayList();
        private boolean mClosed = false;

        public ListComponent() {
        }

        public static ListComponent of(RevisionComponent... components) {
            ListComponent result = new ListComponent();
            RevisionComponent[] var2 = components;
            int var3 = components.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                RevisionComponent component = var2[var4];
                result.add(component);
            }

            return result;
        }

        public void add(RevisionComponent component) {
            this.mItems.add(component);
        }

        public int asInteger() {
            return 0;
        }

        public boolean isPreview() {
            return !this.mItems.isEmpty() && ((RevisionComponent) this.mItems.get(this.mItems.size() - 1)).isPreview();
        }

        public int compareTo(RevisionComponent o) {
            if (o instanceof NumberComponent) {
                return -1;
            } else if (o instanceof StringComponent) {
                return 1;
            } else if (!(o instanceof ListComponent)) {
                return 0;
            } else {
                ListComponent rhs = (ListComponent) o;

                for (int i = 0; i < this.mItems.size() && i < rhs.mItems.size(); ++i) {
                    int rc = ((RevisionComponent) this.mItems.get(i)).compareTo(rhs.mItems.get(i));
                    if (rc != 0) {
                        return rc;
                    }
                }

                return this.mItems.size() - rhs.mItems.size();
            }
        }

        public boolean equals(Object o) {
            return o instanceof ListComponent && ((ListComponent) o).mItems.equals(this.mItems);
        }

        public int hashCode() {
            return this.mItems.hashCode();
        }

        public String toString() {
            return Joiner.on("-").join(this.mItems);
        }
    }

    private static class PlusComponent extends RevisionComponent {
        private PlusComponent() {
        }

        public String toString() {
            return "+";
        }

        public int asInteger() {
            return -1;
        }

        public boolean isPreview() {
            return false;
        }

        public int compareTo(RevisionComponent o) {
            throw new UnsupportedOperationException("Please use a specific comparator that knows how to handle +");
        }
    }

    public static class StringComponent extends RevisionComponent {
        private final String mString;

        public StringComponent(String string) {
            this.mString = string;
        }

        public String toString() {
            return this.mString;
        }

        public int asInteger() {
            return 0;
        }

        public boolean isPreview() {
            return this.mString.startsWith("rc") || this.mString.startsWith("alpha") || this.mString.startsWith("beta") || this.mString.equals("SNAPSHOT");
        }

        public boolean equals(Object o) {
            return o instanceof StringComponent && ((StringComponent) o).mString.equals(this.mString);
        }

        public int hashCode() {
            return this.mString.hashCode();
        }

        public int compareTo(RevisionComponent o) {
            if (o instanceof NumberComponent) {
                return -1;
            } else if (o instanceof StringComponent) {
                return this.mString.compareTo(((StringComponent) o).mString);
            } else {
                return o instanceof ListComponent ? -1 : 0;
            }
        }
    }

    public static class PaddedNumberComponent extends NumberComponent {
        private final String mString;

        public PaddedNumberComponent(int number, String string) {
            super(number);
            this.mString = string;
        }

        public String toString() {
            return this.mString;
        }

        public boolean equals(Object o) {
            return o instanceof PaddedNumberComponent && ((PaddedNumberComponent) o).mString.equals(this.mString);
        }

        public int hashCode() {
            return this.mString.hashCode();
        }
    }

    public static class NumberComponent extends RevisionComponent {
        private final int mNumber;

        public NumberComponent(int number) {
            this.mNumber = number;
        }

        public String toString() {
            return Integer.toString(this.mNumber);
        }

        public int asInteger() {
            return this.mNumber;
        }

        public boolean isPreview() {
            return false;
        }

        public boolean equals(Object o) {
            return o instanceof NumberComponent && ((NumberComponent) o).mNumber == this.mNumber;
        }

        public int hashCode() {
            return this.mNumber;
        }

        public int compareTo(RevisionComponent o) {
            if (o instanceof NumberComponent) {
                return this.mNumber - ((NumberComponent) o).mNumber;
            } else if (o instanceof StringComponent) {
                return 1;
            } else {
                return o instanceof ListComponent ? 1 : 0;
            }
        }
    }

    public abstract static class RevisionComponent implements Comparable<RevisionComponent> {
        public RevisionComponent() {
        }

        public abstract int asInteger();

        public abstract boolean isPreview();
    }

    public static enum ArtifactType {
        POM("pom"),
        JAR("jar"),
        MAVEN_PLUGIN("maven-plugin"),
        EJB("ejb"),
        WAR("war"),
        EAR("ear"),
        RAR("rar"),
        PAR("par"),
        AAR("aar");

        private final String mId;

        private ArtifactType(String id) {
            this.mId = id;
        }

        public static ArtifactType getArtifactType(String name) {
            if (name != null) {
                ArtifactType[] var1 = values();
                int var2 = var1.length;

                for (int var3 = 0; var3 < var2; ++var3) {
                    ArtifactType type = var1[var3];
                    if (type.mId.equalsIgnoreCase(name)) {
                        return type;
                    }
                }
            }

            return null;
        }

        public String toString() {
            return this.mId;
        }
    }
}
