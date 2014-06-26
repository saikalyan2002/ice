package org.jbei.ice.lib.search;

import org.jbei.ice.lib.AccountCreator;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.dao.hibernate.HibernateUtil;
import org.jbei.ice.lib.dto.entry.PlasmidData;
import org.jbei.ice.lib.dto.search.SearchQuery;
import org.jbei.ice.lib.dto.search.SearchResults;
import org.jbei.ice.lib.entry.EntryCreator;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.shared.BioSafetyOption;
import org.jbei.ice.servlet.InfoToModelFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Hector Plahar
 */
public class SearchControllerTest {

    private SearchController controller;

    @Before
    public void setUp() throws Exception {
        HibernateUtil.initializeMock();
        HibernateUtil.beginTransaction();
        controller = new SearchController();
    }

    @After
    public void tearDown() throws Exception {
        HibernateUtil.commitTransaction();
    }

    @Test
    public void testRunSearch() throws Exception {
        Account account = AccountCreator.createTestAccount("testRunSearch", false);
        Assert.assertNotNull(account);

        // create entry
        PlasmidData data = new PlasmidData();
        data.setCircular(true);
        data.setPromoters("pTet");
        data.setBioSafetyLevel(BioSafetyOption.LEVEL_ONE.ordinal());
        data.setOriginOfReplication("oRep");
        data.setStatus("Complete");
        data.setName("testPlasmid");
        data.setFundingSource("DOE");
        data.setPrincipalInvestigator("Nathan");
        Entry entry = InfoToModelFactory.infoToEntry(data);
        entry = new EntryCreator().createEntry(account, entry);
        Assert.assertNotNull(entry);
        Assert.assertTrue(entry.getId() > 0);
        HibernateUtil.commitTransaction();   // commit triggers indexing

        HibernateUtil.beginTransaction();
        SearchQuery query = new SearchQuery();
        query.setQueryString("testPlasmid");
        SearchResults results = controller.runSearch(account.getEmail(), query, false);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getResultCount());

        // search for promoters
        query.setQueryString("pTet");
        results = controller.runSearch(account.getEmail(), query, false);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getResultCount());

        // search email
        query.setQueryString(account.getEmail());
        results = controller.runSearch(account.getEmail(), query, false);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getResultCount());

        // fake search
        query.setQueryString("FAKE_SEARCH");
        results = controller.runSearch(account.getEmail(), query, false);
        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.getResultCount());
    }
}
