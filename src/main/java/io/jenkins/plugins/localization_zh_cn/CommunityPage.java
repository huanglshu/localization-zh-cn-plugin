package io.jenkins.plugins.localization_zh_cn;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import org.jenkinsci.Symbol;
import org.jvnet.localizer.ResourceBundleHolder;

import edu.umd.cs.findbugs.annotations.CheckForNull;

@Extension
@Symbol("chinese")
public class CommunityPage implements UnprotectedRootAction {
    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return ResourceBundleHolder.get(UserCommunityProperty.class).format("Community.Page");
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "chinese";
    }
}
