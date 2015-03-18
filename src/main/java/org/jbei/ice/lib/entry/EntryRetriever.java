package org.jbei.ice.lib.entry;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbei.ice.lib.access.Permission;
import org.jbei.ice.lib.account.AccountType;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOFactory;
import org.jbei.ice.lib.dao.hibernate.AccountDAO;
import org.jbei.ice.lib.dao.hibernate.EntryDAO;
import org.jbei.ice.lib.dao.hibernate.HibernateUtil;
import org.jbei.ice.lib.dto.entry.AutoCompleteField;
import org.jbei.ice.lib.dto.entry.EntryType;
import org.jbei.ice.lib.dto.entry.PartData;
import org.jbei.ice.lib.dto.entry.Visibility;
import org.jbei.ice.lib.dto.folder.FolderAuthorization;
import org.jbei.ice.lib.dto.permission.AccessPermission;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.folder.Folder;
import org.jbei.ice.lib.group.Group;
import org.jbei.ice.lib.group.GroupController;
import org.jbei.ice.lib.shared.ColumnField;
import org.jbei.ice.lib.utils.IceCSVSerializer;

import java.util.*;

/**
 * @author Hector Plahar
 */
public class EntryRetriever {

    private final EntryDAO dao;
    private final EntryAuthorization authorization;

    public EntryRetriever() {
        this.dao = DAOFactory.getEntryDAO();
        authorization = new EntryAuthorization();
    }

    public String getListAsCSV(String userId, ArrayList<Long> list) {  // todo : use a file for large lists
        if (list == null || list.isEmpty() || userId.isEmpty())
            return "";

        List<Entry> entryList = new LinkedList<>();

        for (Number item : list) {
            Entry entry = this.dao.get(item.longValue());
            if (entry == null || !authorization.canRead(userId, entry))
                continue;

            entryList.add(entry);
        }

        return IceCSVSerializer.serializeList(entryList);
    }

    public String getAsCSV(String userId, String id) {
        Entry entry = getEntry(id);
        if (entry == null)
            return null;

        authorization.expectRead(userId, entry);
        return IceCSVSerializer.serialize(entry);
    }

    public String getPartNumber(String userId, String id) {
        Entry entry = getEntry(id);
        if (entry == null)
            return null;

        authorization.expectRead(userId, entry);
        return entry.getPartNumber();
    }

    protected Entry getEntry(String id) {
        Entry entry = null;

        // check if numeric
        try {
            entry = dao.get(Long.decode(id));
        } catch (NumberFormatException nfe) {
            // fine to ignore
        }

        // check for part Id
        if (entry == null)
            entry = dao.getByPartNumber(id);

        // check for global unique id
        if (entry == null)
            entry = dao.getByRecordId(id);

        return entry;
    }

    public ArrayList<AccessPermission> getEntryPermissions(String userId, String id) {
        Entry entry = getEntry(id);
        if (entry == null)
            return null;

        // viewing permissions requires write permissions
        authorization.expectWrite(userId, entry);

        ArrayList<AccessPermission> accessPermissions = new ArrayList<>();
        Set<Permission> permissions = DAOFactory.getPermissionDAO().getEntryPermissions(entry);

        GroupController groupController = new GroupController();
        Group publicGroup = groupController.createOrRetrievePublicGroup();
        for (Permission permission : permissions) {
            if (permission.getAccount() == null && permission.getGroup() == null)
                continue;
            if (permission.getGroup() != null && permission.getGroup() == publicGroup)
                continue;
            accessPermissions.add(permission.toDataTransferObject());
        }

        return accessPermissions;
    }

    // return list of part data with only partId and id filled in
    public ArrayList<PartData> getMatchingPartNumber(String token, int limit) {
        if (token == null)
            return new ArrayList<>();

        token = token.replaceAll("'", "");
        ArrayList<PartData> dataList = new ArrayList<>();
        for (Entry entry : dao.getMatchingEntryPartNumbers(token, limit)) {
            EntryType type = EntryType.nameToType(entry.getRecordType());
            PartData partData = new PartData(type);
            partData.setId(entry.getId());
            partData.setPartId(entry.getPartNumber());
            partData.setName(entry.getName());
            dataList.add(partData);
        }
        return dataList;
    }

    public Set<String> getMatchingAutoCompleteField(AutoCompleteField field, String token, int limit) {
        token = token.replaceAll("'", "");
        Set<String> results;
        switch (field) {
            case SELECTION_MARKERS:
                results = dao.getMatchingSelectionMarkers(token, limit);
                break;

            case ORIGIN_OF_REPLICATION:
                results = dao.getMatchingOriginOfReplication(token, limit);
                break;

            case PROMOTERS:
                results = dao.getMatchingPromoters(token, limit);
                break;

            case REPLICATES_IN:
                results = dao.getMatchingReplicatesIn(token, limit);
                break;

            case PLASMID_NAME:
                results = dao.getMatchingPlasmidPartNumbers(token, limit);
                break;

            case PLASMID_PART_NUMBER:
                results = dao.getMatchingPlasmidPartNumbers(token, limit);
                break;

            default:
                results = new HashSet<>();
        }

        // process to remove commas
        HashSet<String> individualResults = new HashSet<>();
        for (String result : results) {
            for (String split : result.split(",")) {
                individualResults.add(split.trim());
            }
        }
        return individualResults;
    }

    /**
     * Retrieve {@link Entry} from the database by id.
     *
     * @param userId account identifier of user performing action
     * @param id     unique local identifier for entry
     * @return entry retrieved from the database.
     */
    public Entry get(String userId, long id) {
        Entry entry = dao.get(id);
        if (entry == null)
            return null;

        authorization.expectRead(userId, entry);
        return entry;
    }

    public String getEntrySummary(long id) {
        return dao.getEntrySummary(id);
    }

    public List<Long> getEntriesFromSelectionContext(String userId, EntrySelection context) {
        boolean all = context.isAll();
        EntryType entryType = context.getEntryType();

        switch (context.getSelectionType()) {
            default:
            case FOLDER:
                if(!context.getEntries().isEmpty()){
                    return context.getEntries();
                }else{
                    long folderId = Long.decode(context.getFolderId());
                    return getFolderEntries(userId, folderId, all, entryType);
                }

            case SEARCH:
                break;

            case COLLECTION:
                if(!context.getEntries().isEmpty()){
                    return context.getEntries();
                }else {
                    return getCollectionEntries(userId, context.getFolderId(), all, entryType);
                }
        }

        return null;

    }

    protected List<Long> getCollectionEntries(String userId, String collection, boolean all, EntryType type) {
        List<Long> entries = null;
        Account account = new AccountDAO().getByEmail(userId);

        Set<Group> accountGroups = new HashSet<>(account.getGroups());
        GroupController controller = new GroupController();
        Group everybodyGroup = controller.createOrRetrievePublicGroup();
        accountGroups.add(everybodyGroup);

        switch (collection.toLowerCase()) {
            case "personal":
                if (all)
                    type = null;
                entries = dao.getOwnerEntryIds(userId, type);
                break;
            case "shared":
                List<Entry> es = dao.sharedWithUserEntries(account,
                                                    accountGroups,
                                                    ColumnField.CREATED,
                                                    true,
                                                    0,
                                                    (int) dao.getAllEntryCount());
                entries = new ArrayList<>();
                for(Entry e : es){
                    entries.add(e.getId());
                }
                break;
            case "available":
                entries = getVisibleEntries(account.getType() == AccountType.ADMIN);
                break;
        }

        return entries;
    }

    protected List<Long> getVisibleEntries(boolean admin) {
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            Query query;

            if (admin){
                query = session.createQuery("SELECT e.id FROM Entry e WHERE (visibility IS NULL OR visibility = " +
                    Visibility.OK.getValue() + " OR visibility = " + Visibility.PENDING.getValue() + ")");
            }else{
                query = session.createQuery("SELECT DISTINCT e.id FROM Entry e, Permission p" +
                        " WHERE p.group = :group AND e = p.entry AND e.visibility = :v");
                query.setParameter("group", new GroupController().createOrRetrievePublicGroup());
                query.setParameter("v", Visibility.OK.getValue());
            }

            return query.list();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new RuntimeException(he);
        }
    }

    // todo : folder controller
    protected List<Long> getFolderEntries(String userId, long folderId, boolean all, EntryType type) {
        Folder folder = DAOFactory.getFolderDAO().get(folderId);
        FolderAuthorization folderAuthorization = new FolderAuthorization();
        folderAuthorization.expectRead(userId, folder);

        if (all)
            type = null;
        return DAOFactory.getFolderDAO().getFolderContentIds(folderId, type);
    }
}
