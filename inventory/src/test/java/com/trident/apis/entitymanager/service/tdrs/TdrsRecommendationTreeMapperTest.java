package com.trident.apis.entitymanager.service.tdrs;

import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.shared.immigration.dto.tdrs.TdrsDocumentDto;
import com.trident.shared.immigration.dto.tdrs.recommendation.*;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static com.trident.apis.entitymanager.MockDataProvider.getTdrsDocumentId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Category(CouchbaseIntegrationTest.class)
public class TdrsRecommendationTreeMapperTest extends MockMvcTestBase {

    @Autowired
    private TdrsRecommendationTreeMapper mapper;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Before
    public void setUp() {
        mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_DOCUMENT);
        mockDataProvider.insertMock(MockDataProvider.MockType.TDRS_DOCUMENT, 5);
    }

    @After
    public void tearDown() {
        mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_DOCUMENT);
    }

    @Test
    public void testToFlatRecommendation() throws Exception {
        RecommendationNode result = mapper.toFlatRecommendation(createDtoLogicNode());
        Assert.assertThat(result, Matchers.instanceOf(LogicNode.class));

        Set<RecommendationNode> rootChilds = ((LogicNode) result).getList();

        Assert.assertThat(rootChilds, hasItem(CoreMatchers.instanceOf(DocumentNode.class)));
        Assert.assertThat(rootChilds, hasItem(CoreMatchers.instanceOf(LogicNode.class)));
        Assert.assertThat(rootChilds, hasSize(2));

        DocumentNode documentNode1 = rootChilds.stream()
                .filter(recommendationNode -> recommendationNode instanceof DocumentNode)
                .map(recommendationNode -> (DocumentNode) recommendationNode)
                .findFirst().get();
        LogicNode logicNode1 = rootChilds.stream()
                .filter(recommendationNode -> recommendationNode instanceof LogicNode)
                .map(recommendationNode -> (LogicNode) recommendationNode)
                .findFirst().get();

        Assert.assertThat(documentNode1.getDocumentId(), Matchers.equalTo(getTdrsDocumentId(1)));
        Assert.assertThat(logicNode1.getType(), Matchers.is(LogicNodeType.AND));
        Assert.assertThat(logicNode1.getList(), Matchers.hasItem(new DocumentNode(getTdrsDocumentId(2))));

        Object logicNode2 = logicNode1.getList().stream()
                .filter(recommendationNode -> recommendationNode instanceof LogicNode)
                .map(recommendationNode -> (LogicNode) recommendationNode)
                .findFirst().get();
        Assert.assertThat(logicNode2, Matchers.instanceOf(LogicNode.class));
        Assert.assertThat(((LogicNode) logicNode2).getType(), Matchers.is(LogicNodeType.OR));
    }

    @Test
    public void testFullRecommendation() {
        RecommendationNode result = mapper.toFullRecommendation(createLogicNode());
        Assert.assertThat(result, Matchers.instanceOf(LogicNode.class));

        LogicNode root = (LogicNode) result;
        Assert.assertThat(root.getList(), hasItem(CoreMatchers.instanceOf(DocumentNodeDto.class)));
        Assert.assertThat(root.getList(), not(hasItem(CoreMatchers.instanceOf(DocumentNode.class))));
        Assert.assertThat(root.getList(), hasItem(CoreMatchers.instanceOf(LogicNode.class)));
        Assert.assertThat(root.getList(), hasSize(2));
        DocumentNodeDto documentNode1 = root.getList().stream()
                .filter(recommendationNode -> recommendationNode instanceof DocumentNodeDto)
                .map(recommendationNode -> (DocumentNodeDto) recommendationNode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Document Node DTO not found."));
        Assert.assertThat(documentNode1.getDocument().getId(), Matchers.equalTo(getTdrsDocumentId(1)));

        LogicNode logicNode1 = root.getList().stream()
                .filter(recommendationNode -> recommendationNode instanceof LogicNode)
                .map(recommendationNode -> (LogicNode) recommendationNode)
                .filter(logicNode -> logicNode.getType() == LogicNodeType.AND)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Logic node AND not found."));
        Assert.assertThat(logicNode1.getList(), hasSize(2));

        LogicNode logicNode2 = logicNode1.getList().stream()
                .filter(recommendationNode -> recommendationNode instanceof LogicNode)
                .map(recommendationNode -> (LogicNode) recommendationNode)
                .filter(logicNode -> logicNode.getType() == LogicNodeType.OR)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Logic node OR not found."));
        Assert.assertThat(logicNode2.getList(), hasSize(2));
    }

    @Test
    public void getDocumentIds() {
        List<String> ids = mapper.getDocumentIds(createLogicNode());
        assertThat(ids, notNullValue());
        assertThat(ids.size(), is(4));
        IntStream.range(1, 5).forEach(i -> {
            assertThat(ids, hasItem(getTdrsDocumentId(i)));
        });
    }

    /*
     * LN - LogicNode, DN - DocumentNode
     *
     * LN1
     * DN1 - or - LN2
     * LN3 - and - DN2
     * DN3 - or - DN4
     */
    private RecommendationNode createLogicNode() {
        LogicNode orLogicNode3 = LogicNode.or(new DocumentNode(getTdrsDocumentId(3)), new DocumentNode(getTdrsDocumentId(4)));
        LogicNode andLogicNode2 = LogicNode.and(orLogicNode3, new DocumentNode(getTdrsDocumentId(2)));
        LogicNode orLogicNode1 = LogicNode.or(new DocumentNode(getTdrsDocumentId(1)), andLogicNode2);
        return orLogicNode1;

    }

    private RecommendationNode createDtoLogicNode() {
        LogicNode orLogicNode3 = LogicNode.or(createDtoDocumentNode(3), createDtoDocumentNode(4));
        LogicNode andLogicNode2 = LogicNode.and(orLogicNode3, createDtoDocumentNode(2));
        LogicNode orLogicNode1 = LogicNode.or(createDtoDocumentNode(1), andLogicNode2);
        return orLogicNode1;
    }

    private RecommendationNode createDtoDocumentNode(int id) {
        TdrsDocumentDto tdrsDocumentDto = new TdrsDocumentDto();
        tdrsDocumentDto.setId(getTdrsDocumentId(id));
        tdrsDocumentDto.setName("name" + id);
        tdrsDocumentDto.setDescription("description" + id);
        tdrsDocumentDto.setPolarCode("polarCode" + id);
        return new DocumentNodeDto(tdrsDocumentDto);
    }

}
