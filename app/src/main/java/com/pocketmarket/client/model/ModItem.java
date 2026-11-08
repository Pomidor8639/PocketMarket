package com.pocketmarket.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ModItem implements Serializable {
    private String id;
    private String title;
    private String category;
    private String categoryName;
    private String fileType;
    private String version;
    private String mcVersion;
    private String author;
    private String fileSize;
    private long fileSizeBytes;
    private int downloads;
    private int views;
    private int likes;
    private boolean isLiked;
    private double rating;
    private int ratingsCount;
    private String shortDescription;
    private String description;
    private String icon;
    private List<String> screenshots;
    private String fileUrl;
    private String installType;
    private String packageName;
    private String changelog;
    private List<String> tags;
    private List<Review> reviews;

    public ModItem() {
        this.screenshots = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.reviews = new ArrayList<>();
    }

    public boolean isApk() {
        return "apks".equalsIgnoreCase(category) || "apk".equalsIgnoreCase(fileType);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category != null ? category : "mods";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryName() {
        if (categoryName != null) return categoryName;
        if ("apks".equalsIgnoreCase(category)) return "АПК Моды";
        if ("textures".equalsIgnoreCase(category)) return "Текстурпаки";
        return "Моды";
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getFileType() {
        return fileType != null ? fileType : (isApk() ? "apk" : "mcpack");
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getVersion() {
        return version != null ? version : "1.0";
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMcVersion() {
        return mcVersion != null ? mcVersion : "1.20+";
    }

    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public String getAuthor() {
        return author != null ? author : "Community";
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFileSize() {
        return fileSize != null ? fileSize : "10 MB";
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(int ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public String getShortDescription() {
        return shortDescription != null ? shortDescription : "";
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getScreenshots() {
        return screenshots != null ? screenshots : new ArrayList<>();
    }

    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getInstallType() {
        return installType != null ? installType : (isApk() ? "apk_package_installer" : "intent_minecraft");
    }

    public void setInstallType(String installType) {
        this.installType = installType;
    }

    public String getPackageName() {
        return packageName != null ? packageName : "";
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Review> getReviews() {
        return reviews != null ? reviews : new ArrayList<>();
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }
}
