package com.trident.apis.entitymanager.repository;

import com.couchbase.client.java.query.*;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.functions.AggregateFunctions;
import com.couchbase.client.java.query.dsl.path.GroupByPath;
import com.couchbase.client.java.query.dsl.path.LimitPath;
import com.couchbase.client.java.query.dsl.path.WherePath;
import com.google.common.collect.Lists;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.repository.query.CountFragment;
import org.springframework.data.couchbase.repository.query.support.N1qlUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by dimzul on 11/24/16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Category(CouchbaseIntegrationTest.class)
public abstract class BaseRepositoryTest {

    private static final int MOCK_COUNT = 5;
    private static final String WHERE = RepositoryDecorator.TYPE_KEY + "='%s'";

    @Autowired
    private MockDataProvider mockDataProvider;
    @Autowired
    private CouchbaseTemplate couchbaseTemplate;

    protected abstract MockDataProvider.MockType getType();

    protected abstract int getMockCount();

    protected abstract RepositoryDecorator getRepository();

    protected abstract String getModelCouchbaseName();

    protected abstract Class getModelClass();

    @BeforeClass
    public static void init() {
        RepositoryDecorator.setPrefix("test_dive_");
    }

    @Before
    public void setUp() throws Exception {
        mockDataProvider.cleanDb(getType());
        mockDataProvider.insertMock(getType(), getMockCount());
    }

    @After
    public void tearDown() {
        mockDataProvider.cleanDb(getType());
    }

    @Test
    public void testPaging() throws Exception {
        final int size = 3;
        Page page = getRepository().findAll(new PageRequest(1, size), new SearchCriteriaList());
        Assert.assertThat(page.getSize(), Matchers.is(size));
    }

    @Test
    public void tryManualPaging() {
        Pageable pageable = new PageRequest(1, 3);

        ScanConsistency consistency = couchbaseTemplate.getDefaultConsistency().n1qlConsistency();


        Statement countStatement = Select.select(new Expression[]{AggregateFunctions.count("*").as("count")})
                .from(couchbaseTemplate.getCouchbaseBucket().name())
                .where(String.format(WHERE, getModelCouchbaseName()));

        SimpleN1qlQuery countQuery = N1qlQuery.simple(countStatement, N1qlParams.build().consistency(consistency));

        List countResult = couchbaseTemplate.findByN1QLProjection(countQuery, CountFragment.class);
        long totalCount = countResult != null && !countResult.isEmpty() ? ((CountFragment) countResult.get(0)).count : 0L;
        WherePath selectFrom = N1qlUtils.createSelectFromForEntity(couchbaseTemplate.getCouchbaseBucket().name());
        GroupByPath groupBy = selectFrom.where(String.format(WHERE, getModelCouchbaseName()));
        Object limitPath = groupBy;
        if (pageable.getSort() != null) {
            com.couchbase.client.java.query.dsl.Sort[] pageStatement = N1qlUtils.createSort(pageable.getSort(), couchbaseTemplate.getConverter());
            limitPath = groupBy.orderBy(pageStatement);
        }

        Statement pageStatement1 = ((LimitPath) limitPath).limit(pageable.getPageSize()).offset(pageable.getOffset());
        SimpleN1qlQuery query = N1qlQuery.simple(pageStatement1, N1qlParams.build().consistency(consistency));
        List<?> pageContent = couchbaseTemplate.findByN1QL(query, getModelClass());
        Page<?> page = new PageImpl<>(pageContent, pageable, totalCount);

        assertThat(page.getTotalElements(), is((long) getMockCount()));
        assertThat(page.getTotalPages(), is(2));
    }

}
