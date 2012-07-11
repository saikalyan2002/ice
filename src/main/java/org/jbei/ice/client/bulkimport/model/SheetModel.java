package org.jbei.ice.client.bulkimport.model;

import org.jbei.ice.shared.dto.EntryInfo;

// cell data to entry info
public abstract class SheetModel<T extends EntryInfo> {

    public abstract T setInfoField(SheetCellData datum, EntryInfo info);

    public abstract T createInfo();
}
