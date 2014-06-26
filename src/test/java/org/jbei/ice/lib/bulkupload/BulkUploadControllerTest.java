package org.jbei.ice.lib.bulkupload;

import java.util.ArrayList;

import org.jbei.ice.lib.AccountCreator;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.dao.DAOFactory;
import org.jbei.ice.lib.dao.hibernate.EntryDAO;
import org.jbei.ice.lib.dao.hibernate.HibernateUtil;
import org.jbei.ice.lib.dto.bulkupload.EntryField;
import org.jbei.ice.lib.dto.bulkupload.PreferenceInfo;
import org.jbei.ice.lib.dto.entry.EntryType;
import org.jbei.ice.lib.dto.entry.PartData;
import org.jbei.ice.lib.dto.entry.Visibility;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.shared.BioSafetyOption;
import org.jbei.ice.lib.shared.StatusType;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Hector Plahar
 */
public class BulkUploadControllerTest {

    private BulkUploadController controller;

    @Before
    public void setUp() throws Exception {
        HibernateUtil.initializeMock();
        HibernateUtil.beginTransaction();
        controller = new BulkUploadController();
    }

    @After
    public void tearDown() throws Exception {
        HibernateUtil.commitTransaction();
    }

    @Test
    public void testRetrievePendingImports() throws Exception {
        Account account = AccountCreator.createTestAccount("testRetrievePendingImports", true);
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.PLASMID);
        autoUpdate.setRow(0);
        autoUpdate.getKeyValue().put(EntryField.NAME, "JBEI-0001");
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "this is a test");
        autoUpdate.getKeyValue().put(EntryField.PI, "test");
        autoUpdate.getKeyValue().put(EntryField.SELECTION_MARKERS, "select");
        autoUpdate.getKeyValue().put(EntryField.STATUS, StatusType.COMPLETE.toString());
        autoUpdate.getKeyValue().put(EntryField.BIOSAFETY_LEVEL, BioSafetyOption.LEVEL_TWO.getValue());

        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.PLASMID);
        Assert.assertNotNull(autoUpdate);
        Assert.assertTrue(autoUpdate.getEntryId() > 0);
        Assert.assertTrue(autoUpdate.getBulkUploadId() > 0);
        Assert.assertTrue(autoUpdate.getLastUpdate() != null);

        // check that the bulk upload has been created
        BulkUploadInfo info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0);
        Assert.assertNotNull("Null bulk upload", info);

        Assert.assertTrue("Submitting draft", controller.submitBulkImportDraft(account, autoUpdate.getBulkUploadId()));

        // entries associated with bulk upload must be pending
        info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 10);
        Assert.assertNotNull(info);
        Assert.assertTrue("Invalid entry count", info.getEntryList().size() == 1);
        Assert.assertTrue(info.getEntryList().get(0).getVisibility() == Visibility.PENDING);

        ArrayList<BulkUploadInfo> pending = controller.retrievePendingImports(account);
        Assert.assertNotNull("Null pending import", pending);
        boolean b = false;
        for (BulkUploadInfo uploadInfo : pending) {
            if (uploadInfo.getAccount().getEmail().equals(account.getEmail())) {
                b = true;
                break;
            }
        }
        Assert.assertTrue(b);
    }

    @Test
    public void testRetrieveById() throws Exception {
        Account account = AccountCreator.createTestAccount("testRetrieveById", false);
        Assert.assertNull(controller.retrieveById(account.getEmail(), 100l, 0, 1));

        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.PLASMID);
        autoUpdate.getKeyValue().put(EntryField.NAME, "JBEI-0001");
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "this is a test");
        autoUpdate.getKeyValue().put(EntryField.PI, "test");
        autoUpdate.getKeyValue().put(EntryField.SELECTION_MARKERS, "select");
        autoUpdate.getKeyValue().put(EntryField.STATUS, StatusType.COMPLETE.toString());
        autoUpdate.getKeyValue().put(EntryField.BIOSAFETY_LEVEL, BioSafetyOption.LEVEL_TWO.getValue());

        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.PLASMID);
        Assert.assertNotNull(autoUpdate);
        Assert.assertTrue(autoUpdate.getEntryId() > 0);
        Assert.assertTrue(autoUpdate.getBulkUploadId() > 0);
        Assert.assertTrue(autoUpdate.getLastUpdate() != null);

        // check that the bulk upload has been created
        BulkUploadInfo info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0);
        Assert.assertNotNull(info);
        Assert.assertEquals(0, info.getEntryList().size());
        info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 10);
        Assert.assertNotNull(info);
        Assert.assertEquals(1, info.getEntryList().size());
    }

    @Test
    public void testRetrieveByUser() throws Exception {
        Account account = AccountCreator.createTestAccount("testRetrieveByUser", false);
        ArrayList<BulkUploadInfo> results = controller.retrieveByUser(account, account);
        Assert.assertEquals(0, results.size());

        // create strain with plasmid
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.STRAIN);
        autoUpdate.getKeyValue().put(EntryField.NAME, "strainPlasmid");
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "strainPlasmidSummary");
        autoUpdate.getKeyValue().put(EntryField.BIOSAFETY_LEVEL, "Level 2");
        autoUpdate.getKeyValue().put(EntryField.STATUS, "In Progress");
        autoUpdate.getKeyValue().put(EntryField.PI, "Principal Investigator");
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "strain summary");
        autoUpdate.getKeyValue().put(EntryField.SELECTION_MARKERS, "strain selection markers");
        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.STRAIN);
        Assert.assertNotNull(autoUpdate);
        ArrayList<BulkUploadInfo> userUpload = controller.retrieveByUser(account, account);
        Assert.assertNotNull(userUpload);
        Assert.assertEquals(1, userUpload.size());

        Account account2 = AccountCreator.createTestAccount("testRetrieveByUser2", false);
        int count = 10;
        for (int i = 0; i < count; i += 1) {
            autoUpdate = new BulkUploadAutoUpdate(EntryType.PART);
            autoUpdate.getKeyValue().put(EntryField.NAME, "Name" + i);
            autoUpdate.getKeyValue().put(EntryField.PI, "PI" + i);
            autoUpdate.getKeyValue().put(EntryField.SUMMARY, "Summary" + i);
            if (i % 2 == 0)
                Assert.assertNotNull(controller.autoUpdateBulkUpload(account2.getEmail(), autoUpdate,
                                                                     EntryType.PART));
            else
                Assert.assertNotNull(controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate,
                                                                     EntryType.PART));
        }
        userUpload = controller.retrieveByUser(account2, account2);
        Assert.assertEquals(count / 2, userUpload.size());
        userUpload = controller.retrieveByUser(account, account);
        Assert.assertEquals(count / 2 + 1, userUpload.size());
    }

    @Test
    public void testDeleteDraftById() throws Exception {
        Account account = AccountCreator.createTestAccount("testDeleteDraftById", false);
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.PLASMID);
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "plasmid summary");
        autoUpdate.getKeyValue().put(EntryField.NAME, "plasmid name");
        autoUpdate.getKeyValue().put(EntryField.PI, "plasmid principal investigator");
        autoUpdate.getKeyValue().put(EntryField.SELECTION_MARKERS, "plasmid select markers");
        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.PLASMID);
        Assert.assertNotNull(autoUpdate);
        BulkUploadInfo info = controller.deleteDraftById(account, autoUpdate.getBulkUploadId());
        Assert.assertNotNull(info);
        Assert.assertEquals(autoUpdate.getBulkUploadId(), info.getId());
//        Assert.assertEquals(1, info.getCount());
        Assert.assertNull(controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0));
    }

    @Test
    public void testAutoUpdateBulkUpload() throws Exception {
        EntryType type = EntryType.STRAIN;
        Account account = AccountCreator.createTestAccount("testAutoUpdateBulkUpload", false);
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.STRAIN);
        autoUpdate.getKeyValue().put(EntryField.LINKS, "google");

        // first auto update. expect it to create a new bulk upload and entry
        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, type);
        Assert.assertNotNull(autoUpdate);
        long entryId = autoUpdate.getEntryId();
        long bulkId = autoUpdate.getBulkUploadId();
        Assert.assertTrue(entryId > 0);
        Assert.assertTrue(bulkId > 0);

        BulkUploadInfo bulkUploadInfo = controller.retrieveById(account.getEmail(), bulkId, 0, 1000);
        Assert.assertNotNull(bulkUploadInfo);

        EntryDAO dao = DAOFactory.getEntryDAO();
        Entry entry = dao.get(entryId);
        Assert.assertNotNull(entry);
        Assert.assertNotNull(entry.getLinks());
        Assert.assertEquals(1, entry.getLinks().size());

        autoUpdate = new BulkUploadAutoUpdate(EntryType.PLASMID);

        // auto update: expect plasmid and bulk upload with no fields set
        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, type);
        Assert.assertNotNull(autoUpdate);
        entryId = autoUpdate.getEntryId();
        bulkId = autoUpdate.getBulkUploadId();

        Assert.assertTrue(entryId > 0);
        Assert.assertTrue(bulkId > 0);

        entry = dao.get(entryId);
        Assert.assertNotNull(entry);
    }

    @Test
    public void testSubmitBulkImportDraft() throws Exception {
        Account account = AccountCreator.createTestAccount("testSubmitBulkImportDraft", false);
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.STRAIN);
        autoUpdate.getKeyValue().put(EntryField.NAME, "JBEI-0001");
        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.STRAIN);
        Assert.assertNotNull(autoUpdate);
        Assert.assertTrue(autoUpdate.getEntryId() > 0);
        Assert.assertTrue(autoUpdate.getBulkUploadId() > 0);
        Assert.assertTrue(autoUpdate.getLastUpdate() != null);

        // check that the entry has been created and has visibility of draft
        Entry entry = DAOFactory.getEntryDAO().get(autoUpdate.getEntryId());
        Assert.assertNotNull(entry);
        Assert.assertEquals("JBEI-0001", entry.getName());
        Assert.assertTrue(entry.getVisibility().equals(Integer.valueOf(Visibility.DRAFT.getValue())));


        // check that the bulk upload has been created
        BulkUploadInfo info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0);
        Assert.assertNotNull(info);

        // try to submit. should be rejected because the required fields are not present
        Assert.assertFalse(controller.submitBulkImportDraft(account, autoUpdate.getBulkUploadId()));

        // enter information for others
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "this is a test");
        autoUpdate.getKeyValue().put(EntryField.PI, "test");
        autoUpdate.getKeyValue().put(EntryField.SELECTION_MARKERS, "select");

        // use preference for the status
        PreferenceInfo preference = new PreferenceInfo();
        preference.setAdd(true);
        preference.setKey(EntryField.STATUS.toString());
        preference.setValue("Complete");

        autoUpdate.getKeyValue().put(EntryField.BIOSAFETY_LEVEL, BioSafetyOption.LEVEL_TWO.getValue());
        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.STRAIN);

        Assert.assertTrue(controller.submitBulkImportDraft(account, autoUpdate.getBulkUploadId()));

        // entries associated with bulk upload must be pending
        info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 10);
        Assert.assertNotNull(info);
        Assert.assertTrue(info.getEntryList().size() == 1);
        Assert.assertTrue(info.getEntryList().get(0).getVisibility().equals(Visibility.PENDING));

        // check the data associated with it
        PartData entryInfo = info.getEntryList().get(0);
        Assert.assertEquals(entryInfo.getName(), "JBEI-0001");
        Assert.assertEquals(entryInfo.getShortDescription(), "this is a test");
        Assert.assertEquals(entryInfo.getPrincipalInvestigator(), "test");
        Assert.assertTrue(entryInfo.getSelectionMarkers().contains("select"));
        Assert.assertEquals(entryInfo.getBioSafetyLevel(), new Integer(BioSafetyOption.LEVEL_TWO.getValue()));
        Assert.assertEquals(entryInfo.getStatus(), "Complete");
    }

    @Test
    public void testRevertSubmitted() throws Exception {
        Account account = AccountCreator.createTestAccount("testRevertSubmitted", false);
        Account admin = AccountCreator.createTestAccount("testRevertSubmitted+Admin", true);
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.ARABIDOPSIS);
        autoUpdate.getKeyValue().put(EntryField.NAME, "JBEI-0001");
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "this is a test");
        autoUpdate.getKeyValue().put(EntryField.PI, "test");
        autoUpdate.getKeyValue().put(EntryField.STATUS, StatusType.COMPLETE.toString());
        autoUpdate.getKeyValue().put(EntryField.BIOSAFETY_LEVEL, BioSafetyOption.LEVEL_TWO.getValue());

        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.ARABIDOPSIS);
        Assert.assertNotNull(autoUpdate);
        Assert.assertTrue(autoUpdate.getEntryId() > 0);
        Assert.assertTrue(autoUpdate.getBulkUploadId() > 0);
        Assert.assertTrue(autoUpdate.getLastUpdate() != null);

        Assert.assertNotNull(controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0));

        // try to revert. not submitted
        Assert.assertFalse(controller.revertSubmitted(admin, autoUpdate.getBulkUploadId()));
        Assert.assertTrue(controller.submitBulkImportDraft(account, autoUpdate.getBulkUploadId()));
        BulkUploadInfo info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0);
        Assert.assertNotNull(info);
        Assert.assertTrue(controller.revertSubmitted(admin, autoUpdate.getBulkUploadId()));
    }

    @Test
    public void testApproveBulkImport() throws Exception {
        Account account = AccountCreator.createTestAccount("testApproveBulkImport", true);
        BulkUploadAutoUpdate autoUpdate = new BulkUploadAutoUpdate(EntryType.PLASMID);
        autoUpdate.getKeyValue().put(EntryField.NAME, "JBEI-0001");
        autoUpdate.getKeyValue().put(EntryField.SUMMARY, "this is a test");
        autoUpdate.getKeyValue().put(EntryField.PI, "test");
        autoUpdate.getKeyValue().put(EntryField.SELECTION_MARKERS, "select");
        autoUpdate.getKeyValue().put(EntryField.FUNDING_SOURCE, "JBEI");
        autoUpdate.getKeyValue().put(EntryField.STATUS, StatusType.COMPLETE.toString());
        autoUpdate.getKeyValue().put(EntryField.BIOSAFETY_LEVEL, BioSafetyOption.LEVEL_TWO.getValue());

        autoUpdate = controller.autoUpdateBulkUpload(account.getEmail(), autoUpdate, EntryType.PLASMID);
        Assert.assertNotNull(autoUpdate);
        Assert.assertTrue(autoUpdate.getEntryId() > 0);
        Assert.assertTrue(autoUpdate.getBulkUploadId() > 0);
        Assert.assertTrue(autoUpdate.getLastUpdate() != null);

        // submit draft
        Assert.assertTrue(controller.submitBulkImportDraft(account, autoUpdate.getBulkUploadId()));
        Assert.assertTrue(controller.approveBulkImport(account, autoUpdate.getBulkUploadId()));

        // bulk upload should be deleted
        BulkUploadInfo info = controller.retrieveById(account.getEmail(), autoUpdate.getBulkUploadId(), 0, 0);
        Assert.assertNull(info);

        // entry must still exist and have a visibility of OK
        Entry entry = DAOFactory.getEntryDAO().get(autoUpdate.getEntryId());
        Assert.assertNotNull(entry);
        Assert.assertEquals(Visibility.OK.getValue(), entry.getVisibility().intValue());

        // check the set values of the entry (particularly the preferences)
        Assert.assertEquals("test", entry.getPrincipalInvestigator());
        Assert.assertEquals("JBEI", entry.getFundingSource());
        Assert.assertEquals("JBEI-0001", entry.getName());
    }
}
