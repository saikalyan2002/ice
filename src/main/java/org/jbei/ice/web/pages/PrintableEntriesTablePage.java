package org.jbei.ice.web.pages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.jbei.ice.controllers.AccountController;
import org.jbei.ice.controllers.EntryController;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.utils.JbeiConstants;
import org.jbei.ice.web.IceSession;
import org.jbei.ice.web.common.ViewException;

public class PrintableEntriesTablePage extends ProtectedPage {
    private ArrayList<Entry> entries;

    ResourceReference blankImage;
    ResourceReference attachmentImage;
    ResourceReference sequenceImage;
    ResourceReference sampleImage;

    public PrintableEntriesTablePage(ArrayList<Entry> entries, boolean displayOwner) {
        super();

        this.entries = entries;

        blankImage = new ResourceReference(UnprotectedPage.class,
                UnprotectedPage.IMAGES_RESOURCE_LOCATION + "blank.png");
        attachmentImage = new ResourceReference(UnprotectedPage.class,
                UnprotectedPage.IMAGES_RESOURCE_LOCATION + "attachment.gif");
        sequenceImage = new ResourceReference(UnprotectedPage.class,
                UnprotectedPage.IMAGES_RESOURCE_LOCATION + "sequence.gif");
        sampleImage = new ResourceReference(UnprotectedPage.class,
                UnprotectedPage.IMAGES_RESOURCE_LOCATION + "sample.png");

        add(JavascriptPackageResource.getHeaderContribution(UnprotectedPage.class,
                JS_RESOURCE_LOCATION + "jquery.cluetip.js"));
        add(CSSPackageResource.getHeaderContribution(UnprotectedPage.class,
                STYLES_RESOURCE_LOCATION + "jquery.cluetip.css"));

        if (displayOwner) {
            add(createWithOwnerTableFragment(displayOwner));
        } else {
            add(createWithoutOwnerTableFragment(displayOwner));
        }
    }

    @Override
    protected void initializeComponents() {
        add(new Label("title", "Printable"));
    }

    private Fragment createWithOwnerTableFragment(boolean displayOwner) {
        Fragment fragment = new Fragment("entriesFragmentPanel", "withOwnerFragment", this);

        fragment.add(new Image("attachmentHeaderImage", attachmentImage));
        fragment.add(new Image("sequenceHeaderImage", sequenceImage));
        fragment.add(new Image("sampleHeaderImage", sampleImage));

        fragment.setOutputMarkupPlaceholderTag(true);
        fragment.setOutputMarkupId(true);
        fragment.add(initializeDataView(displayOwner));

        return fragment;
    }

    private Fragment createWithoutOwnerTableFragment(boolean displayOwner) {
        Fragment fragment = new Fragment("entriesFragmentPanel", "withoutOwnerFragment", this);

        fragment.add(new Image("attachmentHeaderImage", attachmentImage));
        fragment.add(new Image("sequenceHeaderImage", sequenceImage));
        fragment.add(new Image("sampleHeaderImage", sampleImage));

        fragment.setOutputMarkupPlaceholderTag(true);
        fragment.setOutputMarkupId(true);
        fragment.add(initializeDataView(displayOwner));

        return fragment;
    }

    private ListView<Entry> initializeDataView(final boolean displayOwner) {
        ListView<Entry> entriesDataView = new ListView<Entry>("entriesDataView", entries) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Entry> item) {
                Entry entry = item.getModelObject();

                item.add(new SimpleAttributeModifier("class", item.getIndex() % 2 == 0 ? "odd_row"
                        : "even_row"));
                item.add(new Label("index", String.valueOf(item.getIndex() + 1)));
                item.add(new Label("recordType", entry.getRecordType()));

                BookmarkablePageLink<String> entryLink = new BookmarkablePageLink<String>(
                        "partIdLink", EntryViewPage.class, new PageParameters("0=" + entry.getId()));
                entryLink.add(new Label("partNumber", entry.getOnePartNumber().getPartNumber()));
                String tipUrl = (String) urlFor(EntryTipPage.class, new PageParameters());
                entryLink.add(new SimpleAttributeModifier("rel", tipUrl + "/" + entry.getId()));
                item.add(entryLink);

                item.add(new Label("name", entry.getOneName().getName()));
                item.add(new Label("description", entry.getShortDescription()));

                if (displayOwner) {
                    Account ownerAccount = null;

                    try {
                        ownerAccount = AccountController.getByEmail(entry.getOwnerEmail());
                    } catch (ControllerException e) {
                        throw new ViewException(e);
                    }

                    BookmarkablePageLink<ProfilePage> ownerProfileLink = new BookmarkablePageLink<ProfilePage>(
                            "ownerProfileLink", ProfilePage.class, new PageParameters("0=about,1="
                                    + entry.getOwnerEmail()));
                    ownerProfileLink.add(new Label("owner", (ownerAccount != null) ? ownerAccount
                            .getFullName() : entry.getOwner()));
                    String ownerAltText = "Profile "
                            + ((ownerAccount == null) ? entry.getOwner() : ownerAccount
                                    .getFullName());
                    ownerProfileLink.add(new SimpleAttributeModifier("title", ownerAltText));
                    ownerProfileLink.add(new SimpleAttributeModifier("alt", ownerAltText));
                    item.add(ownerProfileLink);
                }

                item.add(new Label("status", JbeiConstants.getStatus(entry.getStatus())));

                EntryController entryController = new EntryController(IceSession.get().getAccount());

                try {
                    item
                            .add(new Image("hasAttachment",
                                    (entryController.hasAttachments(entry)) ? attachmentImage
                                            : blankImage));
                    item.add(new Image("hasSequence",
                            (entryController.hasSequence(entry)) ? sequenceImage : blankImage));
                    item.add(new Image("hasSample",
                            (entryController.hasSamples(entry)) ? sampleImage : blankImage));
                } catch (ControllerException e) {
                    throw new ViewException(e);
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");
                String dateString = dateFormat.format(entry.getCreationTime());
                item.add(new Label("date", dateString));
            }
        };

        return entriesDataView;
    }
}
