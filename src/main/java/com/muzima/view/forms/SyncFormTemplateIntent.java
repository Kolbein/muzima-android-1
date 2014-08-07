/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.support.v4.app.FragmentActivity;
import com.muzima.utils.Constants;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants;

public class SyncFormTemplateIntent extends SyncIntent {
    public SyncFormTemplateIntent(FragmentActivity activity, String[] selectedFormsArray) {
        super(activity);
        putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_TEMPLATES);
        putExtra(DataSyncServiceConstants.FORM_IDS, selectedFormsArray);
    }
}
