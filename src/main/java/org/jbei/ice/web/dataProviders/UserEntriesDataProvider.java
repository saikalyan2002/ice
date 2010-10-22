package org.jbei.ice.web.dataProviders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.wicket.model.PropertyModel;
import org.jbei.ice.controllers.EntryController;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.utils.Utils;
import org.jbei.ice.web.IceSession;
import org.jbei.ice.web.common.ViewException;

import edu.emory.mathcs.backport.java.util.Collections;

public class UserEntriesDataProvider extends AbstractEntriesDataProvider {
    private static final long serialVersionUID = 1L;
    private final Account account;

    public UserEntriesDataProvider(Account account) {
        super();

        this.account = account;
        
        // default sort
        setSort("creationTime", false);
    }

    @Override
    public Iterator<Entry> iterator(int first, int count) {
        entries.clear();

        EntryController entryController = new EntryController(IceSession.get().getAccount());

        try {
            ArrayList<Entry> results = entryController.getEntriesByOwner(account.getEmail(), first,
                count);
            if (results != null) {
                entries.addAll(results);
                Collections.sort(entries, new EntryComparator());
            }
        } catch (ControllerException e) {
            throw new ViewException(e);
        }
        return entries.iterator();
    }

    @Override
    public int size() {
        long numberOfEntries = 0;

        EntryController entryController = new EntryController(IceSession.get().getAccount());

        try {
            numberOfEntries = entryController.getNumberOfEntriesByOwner(account.getEmail());
        } catch (ControllerException e) {
            throw new ViewException(e);
        }

        return Utils.safeLongToInt(numberOfEntries);
    }

    private class EntryComparator implements Comparator<Entry> {
        @Override
        public int compare(Entry o1, Entry o2) {

            int result = 0;
            if(getSort() == null)
                return result;
            
            String property = getSort().getProperty();
            
            PropertyModel<Comparable<Object>> model1 = new PropertyModel<Comparable<Object>>(o1, property);
            PropertyModel<Comparable<Object>> model2 = new PropertyModel<Comparable<Object>>(o2, property);
            
            result = model1.getObject().compareTo(model2.getObject());
            
            if (!getSort().isAscending())
                result *= -1;

            return result;
        }

    }

}
