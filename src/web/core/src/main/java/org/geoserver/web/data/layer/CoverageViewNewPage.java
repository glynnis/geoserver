/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;

public class CoverageViewNewPage extends CoverageViewAbstractPage {

    public CoverageViewNewPage(PageParameters params) throws IOException {
        this(params.getString(WORKSPACE), params.getString(COVERAGESTORE), null, null);
    }

    public CoverageViewNewPage(String workspaceName, String storeName, String coverageName,
            CoverageInfo coverageInfo) throws IOException {
        super(workspaceName, storeName, coverageName, coverageInfo);
    }

    protected void onSave() {
        try {
            if (name.equalsIgnoreCase(COVERAGE_VIEW_NAME)) {
                throw new IllegalArgumentException("Make sure to specify a proper coverage name, different that " + COVERAGE_VIEW_NAME);
            }
            final Catalog catalog = getCatalog();
            final CatalogBuilder builder = new CatalogBuilder(catalog);
            final CoverageStoreInfo coverageStoreInfo = catalog.getCoverageStore(storeId);
            CoverageInfo coverageInfo = null;
            final CoverageView coverageView = buildCoverageView();
            List<CoverageBand> coverageBands = coverageView.getCoverageBands(); 
            if (coverageBands == null || coverageBands.isEmpty()) {
                throw new IllegalArgumentException("No output bands have been specified ");
            }
            coverageInfo = coverageView.createCoverageInfo(name, coverageStoreInfo, builder);
            final LayerInfo layerInfo = builder.buildLayer(coverageInfo);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create Coverage View", e);
            error(new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                    .getString());
        }
    }

    protected void onCancel() {
        doReturn(LayerPage.class);
    }

}