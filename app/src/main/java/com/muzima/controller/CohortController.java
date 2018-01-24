/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.CohortMembership;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.CohortMembershipService;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.util.handler.impl.CohortMembershipDeltaHandler;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS;
import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS_DATA;
import static com.muzima.api.model.APIName.DOWNLOAD_COHORT_MEMBERSHIPS;

public class CohortController {
    private static final String TAG = "CohortController";
    private CohortService cohortService;
    private CohortMembershipService cohortMembershipService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;
    private CohortMembershipDeltaHandler deltaHandler;

    public CohortController(CohortService cohortService, LastSyncTimeService lastSyncTimeService,
                            SntpService sntpService, CohortMembershipService cohortMembershipService,
                            CohortMembershipDeltaHandler deltaHandler) {
        this.cohortService = cohortService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.cohortMembershipService = cohortMembershipService;
        this.deltaHandler = deltaHandler;
    }

    public List<Cohort> getAllCohorts() throws CohortFetchException {
        try {
            return cohortService.getAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public int countAllCohorts() throws CohortFetchException {
        try {
            return cohortService.countAllCohorts();
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public List<Cohort> downloadAllCohorts() throws CohortDownloadException {
        try {
            Date lastSyncTimeForCohorts = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS);
            List<Cohort> allCohorts = cohortService.downloadCohortsByNameAndSyncDate(StringUtils.EMPTY, lastSyncTimeForCohorts);

            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getLocalTime());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return allCohorts;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<CohortData> downloadCohortData(String[] cohortUuids) throws CohortDownloadException {
        ArrayList<CohortData> allCohortData = new ArrayList<CohortData>();
        for (String cohortUuid : cohortUuids) {
            allCohortData.add(downloadCohortDataByUuid(cohortUuid));
        }
        return allCohortData;
    }

    public CohortData downloadCohortDataByUuid(String uuid) throws CohortDownloadException {
        try {
            Date lastSyncDate = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid);
            CohortData cohortData = cohortService.downloadCohortDataAndSyncDate(uuid, false, lastSyncDate);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS_DATA, sntpService.getLocalTime(), uuid);
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return cohortData;
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public List<Cohort> downloadCohortsByPrefix(List<String> cohortPrefixes) throws CohortDownloadException {
        List<Cohort> filteredCohorts = new ArrayList<Cohort>();
        try {
            Date lastSyncDateOfCohort;
            LastSyncTime lastSyncTime;
            for (String cohortPrefix : cohortPrefixes) {
                lastSyncDateOfCohort = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, cohortPrefix);
                List<Cohort> cohorts = cohortService.downloadCohortsByNameAndSyncDate(cohortPrefix, lastSyncDateOfCohort);
                List<Cohort> filteredCohortsForPrefix = filterCohortsByPrefix(cohorts, cohortPrefix);
                addUniqueCohorts(filteredCohorts, filteredCohortsForPrefix);
                lastSyncTime = new LastSyncTime(DOWNLOAD_COHORTS, sntpService.getLocalTime(), cohortPrefix);
                lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            }
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
        return filteredCohorts;
    }

    private void addUniqueCohorts(List<Cohort> filteredCohorts, List<Cohort> filteredCohortsForPrefix) {
        for (Cohort fileteredCohortForPrefix : filteredCohortsForPrefix) {
            boolean found = false;
            for (Cohort filteredCohort : filteredCohorts) {
                if (fileteredCohortForPrefix.getUuid().equals(filteredCohort.getUuid())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                filteredCohorts.add(fileteredCohortForPrefix);
            }
        }
    }

    private List<Cohort> filterCohortsByPrefix(List<Cohort> cohorts, String cohortPrefix) {
        ArrayList<Cohort> filteredCohortList = new ArrayList<Cohort>();
        for (Cohort cohort : cohorts) {
            String lowerCaseCohortName = cohort.getName().toLowerCase();
            String lowerCasePrefix = cohortPrefix.toLowerCase();
            if (lowerCaseCohortName.startsWith(lowerCasePrefix)) {
                filteredCohortList.add(cohort);
            }
        }
        return filteredCohortList;
    }

    public void saveAllCohorts(List<Cohort> cohorts) throws CohortSaveException {
        try {
            cohortService.saveCohorts(cohorts);
        } catch (IOException e) {
            throw new CohortSaveException(e);
        }

    }

    public void deleteAllCohorts() throws CohortDeleteException {
        try {
            cohortService.deleteCohorts(cohortService.getAllCohorts());
        } catch (IOException e) {
            throw new CohortDeleteException(e);
        }
    }

    public void deleteCohorts(List<Cohort> cohorts) throws CohortDeleteException {
        try {
            cohortService.deleteCohorts(cohorts);
        } catch (IOException e) {
            throw new CohortDeleteException(e);
        }
    }

    public List<Cohort> getSyncedCohorts() throws CohortFetchException {
        try {

            List<Cohort> cohorts = cohortService.getAllCohorts();
            List<Cohort> syncedCohorts = new ArrayList<Cohort>();
            for (Cohort cohort : cohorts) {
                //TODO: Have a has members method to make this more explicit
                if (hasCohortMembership(cohort)) {
                    syncedCohorts.add(cohort);
                }
            }
            return syncedCohorts;
        } catch (IOException e) {
            throw new CohortFetchException(e);
        }
    }

    public boolean hasCohortMembership(Cohort cohort) {
        try {
            return cohortMembershipService.countCohortMemberships(cohort.getUuid()) > 0;
        } catch (IOException e) {
            return false;
        }
    }
    public boolean isDownloaded(Cohort cohort) {
        try {
            return cohortService.countCohortMembers(cohort.getUuid()) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public int countSyncedCohorts() throws CohortFetchException {
        return getSyncedCohorts().size();
    }

    public void deleteCohortMembers(String cohortUuid) throws CohortReplaceException {
        try {
            cohortService.deleteCohortMembers(cohortUuid);
        } catch (IOException e) {
            throw new CohortReplaceException(e);
        }

    }

    public void addCohortMembers(List<CohortMember> cohortMembers) throws CohortReplaceException {
        try {
            cohortService.saveCohortMembers(cohortMembers);
        } catch (IOException e) {
            throw new CohortReplaceException(e);
        }

    }

    public List<Cohort> downloadCohortByName(String name) throws CohortDownloadException {
        try {
            return cohortService.downloadCohortsByName(name);
        } catch (IOException e) {
            throw new CohortDownloadException(e);
        }
    }

    public void deleteCohortMembers(List<Cohort> allCohorts) throws CohortReplaceException {
        for (Cohort cohort : allCohorts) {
            deleteCohortMembers(cohort.getUuid());
        }
    }

    public void addCohortMembership(CohortMembership membership)
            throws CohortMembershipSaveException {
        try {
            cohortMembershipService.saveCohortMembership(membership);
        } catch (IOException e) {
            throw new CohortMembershipSaveException(e);
        }
    }

    public void addCohortMemberships(List<CohortMembership> membershipList)
            throws CohortMembershipSaveException {
        try {
            cohortMembershipService.saveCohortMemberships(membershipList);
        } catch (IOException e) {
            throw new CohortMembershipSaveException(e);
        }
    }

    public void updateCohortMemberships(List<CohortMembership> membershipList)
            throws CohortMembershipReplaceException {
        try {
            cohortMembershipService.updateCohortMemberships(membershipList);
        } catch (IOException e) {
            throw new CohortMembershipReplaceException(e);
        }
    }

    public ArrayList<ArrayList<CohortMembership>> downloadCohortMemberships(String[] uuids)
            throws CohortMembershipDownloadException, IOException {
        ArrayList<ArrayList<CohortMembership>> membershipList =
                new ArrayList<ArrayList<CohortMembership>>();
        for (String uuid : uuids) {
            List<CohortMembership> localMemberships =
                    cohortMembershipService.getCohortMemberships(uuid);
            if (!localMemberships.isEmpty()) {
                ArrayList<CohortMembership> newMemberships = new ArrayList<CohortMembership>();
                ArrayList<CohortMembership> updatedMemberships = new ArrayList<CohortMembership>();
                List<CohortMembership> downloadedMemberships = downloadCohortMembershipsByUuid(uuid);
                newMemberships =
                        deltaHandler.getNewCollection(downloadedMemberships, localMemberships);
                updatedMemberships =
                        deltaHandler.getUpdatedCollection(downloadedMemberships, localMemberships);
                if (membershipList.isEmpty()) {
                    membershipList.add(newMemberships);
                    membershipList.add(updatedMemberships);
                } else {
                    membershipList.get(0).addAll(newMemberships);
                    membershipList.get(1).addAll(updatedMemberships);
                }
            } else {
                membershipList.add(new ArrayList<CohortMembership>
                        (downloadCohortMembershipsByUuid(uuid)));
                membershipList.add(new ArrayList<CohortMembership>());
            }
        }
        return membershipList;
    }

    public List<CohortMembership> downloadCohortMembershipsByUuid(String uuid)
            throws CohortMembershipDownloadException {
        try {
            Date lastSyncDate =
                    lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORT_MEMBERSHIPS, uuid);
            List<CohortMembership> memberships =
                    cohortMembershipService.downloadCohortMemberships(uuid, lastSyncDate);
            LastSyncTime lastSyncTime = new LastSyncTime
                    (DOWNLOAD_COHORT_MEMBERSHIPS, sntpService.getLocalTime(), uuid);
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return memberships;
        } catch (IOException e) {
            throw new CohortMembershipDownloadException(e);
        }
    }

    public List<CohortMembership> searchCohortMembershipLocally(String term, String cohortUuid)
            throws CohortMembershipLoadException {
        try {
            return StringUtils.isEmpty(cohortUuid)
                    ? cohortMembershipService.searchCohortMemberships(term)
                    : cohortMembershipService.searchCohortMemberships(term, cohortUuid);
        } catch (Exception e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public int countMemberships(String cohortUuid) throws CohortMembershipLoadException {
        try {
            return cohortMembershipService.countCohortMemberships(cohortUuid);
        } catch (IOException e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public List<CohortMembership> getCohortMemberships(String cohortUuid)
            throws CohortMembershipLoadException {
        try {
            return cohortMembershipService.getCohortMemberships(cohortUuid);
        } catch (IOException e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public List<CohortMembership> getCohortMemberships(String cohortUuid, int page, int pageSize)
            throws CohortMembershipLoadException {
        try {
            return cohortMembershipService.getCohortMemberships(cohortUuid, page, pageSize);
        } catch (IOException e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public int countAllCohortMemberships() throws CohortMembershipLoadException {
        try {
            return cohortMembershipService.countAllCohortMemberships();
        } catch (IOException e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public List<CohortMembership> getAllCohortMemberships() throws CohortMembershipLoadException {
        try {
            return cohortMembershipService.getAllCohortMemberships();
        } catch (IOException e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public List<CohortMembership> getCohortMemberships(int page, int pageSize)
            throws CohortMembershipLoadException {
        try {
            return cohortMembershipService.getCohortMemberships(page, pageSize);
        } catch (IOException e) {
            throw new CohortMembershipLoadException(e);
        }
    }

    public static class CohortDownloadException extends Throwable {
        public CohortDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortFetchException extends Throwable {
        public CohortFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortSaveException extends Throwable {
        public CohortSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortDeleteException extends Throwable {
        public CohortDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortReplaceException extends Throwable {
        public CohortReplaceException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortMembershipDownloadException extends Throwable {
        public CohortMembershipDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortMembershipFetchException extends Throwable {
        public CohortMembershipFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortMembershipSaveException extends Throwable {
        public CohortMembershipSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortMembershipDeleteException extends Throwable {
        public CohortMembershipDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class CohortMembershipReplaceException extends Throwable {
        public CohortMembershipReplaceException(Throwable throwable) {
            super(throwable);
        }
    }
    public static class CohortMembershipLoadException extends Throwable {
        public CohortMembershipLoadException(Throwable throwable) {
            super(throwable);
        }
    }
}
