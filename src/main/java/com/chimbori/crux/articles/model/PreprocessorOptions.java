package com.chimbori.crux.articles.model;


public class PreprocessorOptions {

    private boolean stripUnlikelyCandidates = false;

    private boolean removeScriptsStylesForms = true;

    private boolean removeComments = true;


    public PreprocessorOptions() {

    }

    public PreprocessorOptions(boolean stripUnlikelyCandidates, boolean removeScriptsStylesForms, boolean removeComments) {
        this.stripUnlikelyCandidates = stripUnlikelyCandidates;
        this.removeScriptsStylesForms = removeScriptsStylesForms;
        this.removeComments = removeComments;
    }


    public boolean isStripUnlikelyCandidates() {
        return stripUnlikelyCandidates;
    }

    public void setStripUnlikelyCandidates(boolean stripUnlikelyCandidates) {
        this.stripUnlikelyCandidates = stripUnlikelyCandidates;
    }

    public boolean isRemoveScriptsStylesForms() {
        return removeScriptsStylesForms;
    }

    public void setRemoveScriptsStylesForms(boolean removeScriptsStylesForms) {
        this.removeScriptsStylesForms = removeScriptsStylesForms;
    }

    public boolean isRemoveComments() {
        return removeComments;
    }

    public void setRemoveComments(boolean removeComments) {
        this.removeComments = removeComments;
    }

}
