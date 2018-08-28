package anonymous.com.mtcchoir.models;

public class Mp3Item {

    public Mp3Item() {

    }

    public String getmSongName() {
        return mSongName;
    }

    public void setmSongName(String mSongName) {
        this.mSongName = mSongName;
    }

    public String getmAddedUser() {
        return mAddedUser;
    }

    public void setmAddedUser(String mAddedUser) {
        this.mAddedUser = mAddedUser;
    }

    public String getmPath() {
        return mPath;
    }

    public void setmPath(String mPath) {
        this.mPath = mPath;
    }

    private String mSongName;
    private String mAddedUser;
    private String mPath;

    @Override
    public boolean equals(Object obj){
        if (obj == null) {
            return false;
        }

        if (!Mp3Item.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final Mp3Item other = (Mp3Item) obj;
        return this.getmSongName().equals(other.getmSongName()) && this.getmAddedUser().equals(other.getmAddedUser());
    }
}
