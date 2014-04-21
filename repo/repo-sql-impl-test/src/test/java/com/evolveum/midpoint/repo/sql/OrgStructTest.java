/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.ROrgIncorrect;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgStructTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");

    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_STRUCT_OBJECTS_INCORRECT = TEST_DIR + "/org-monkey-island-incorrect.xml";

    private static final String MODIFY_ORG_ADD_REF_FILENAME = TEST_DIR + "/modify-orgStruct-add-orgref.xml";
    private static final String MODIFY_ORG_INCORRECT_ADD_REF_FILENAME = TEST_DIR + "/modify-orgStruct-incorrect-add-orgref.xml";

    private static final String MODIFY_ORG_DELETE_REF_FILENAME = TEST_DIR + "/modify-orgStruct-delete-ref.xml";
    private static final String MODIFY_ORG_INCORRECT_DELETE_REF_FILENAME = TEST_DIR + "/modify-orgStruct-incorrect-delete-ref.xml";

    private static final String MODIFY_ORG_ADD_USER_FILENAME = TEST_DIR + "/modify-orgStruct-add-user.xml";

    private static final String ORG_F001_OID = "00000000-8888-6666-0000-100000000001";
    private static final String ORG_F002_OID = "00000000-8888-6666-0000-100000000002";
    private static final String ORG_F003_OID = "00000000-8888-6666-0000-100000000003";
    private static final String ORG_F004_OID = "00000000-8888-6666-0000-100000000004";
    private static final String ORG_F005_OID = "00000000-8888-6666-0000-100000000005";
    private static final String ORG_F006_OID = "00000000-8888-6666-0000-100000000006";
    private static final String ORG_F007_OID = "00000000-8888-6666-0000-100000000007";
    private static final String ORG_F008_OID = "00000000-8888-6666-0000-100000000008";
    private static final String ORG_F009_OID = "00000000-8888-6666-0000-100000000009";
    private static final String ORG_F010_OID = "00000000-8888-6666-0000-100000000010";
    private static final String ORG_F012_OID = "00000000-8888-6666-0000-100000000012";


    private static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";

    private static final String MODIFY_ORG_ADD_REF_OID = "00000000-8888-6666-0000-100000000005";
    private static final String MODIFY_ORG_INCORRECT_ADD_REF_OID = "00000000-8888-6666-0000-100000000006";

    private static final String MODIFY_ORG_DELETE_REF_OID = "00000000-8888-6666-0000-100000000006";
    private static final String MODIFY_ORG_INCORRECT_DELETE_REF_OID = "00000000-8888-6666-0000-100000000006";

    private static final String DELETE_ORG_OID = "00000000-8888-6666-0000-100000000006";

    private static final String SEARCH_ORG_OID_UNBOUNDED_DEPTH = "00000000-8888-6666-0000-100000000001";
    private static final String SEARCH_ORG_OID_DEPTH1 = "00000000-8888-6666-0000-100000000001";

    private static final String MODIFY_USER_DELETE_REF_OID = "00000000-8888-6666-0000-100000000002";

    private static final Trace LOGGER = TraceManager.getTrace(OrgStructTest.class);

    String ELAINE_OID;
    private static final String ELAINE_NAME = "elaine";
    private static final String ELAINE_NAME1 = "elaine1";

    @SuppressWarnings({"unchecked", "unused"})
    @Test
    public void test001addOrgStructObjects() throws Exception {

        LOGGER.info("===[ addOrgStruct ]===");
        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");
        List<PrismObject<? extends Objectable>> orgStruct = prismContext.parseObjects(
                new File(ORG_STRUCT_OBJECTS));  

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }

        List<PrismObject<OrgType>> orgTypes = repositoryService.searchObjects(OrgType.class, new ObjectQuery(), null, opResult);
        AssertJUnit.assertNotNull(orgTypes);
        AssertJUnit.assertEquals(9, orgTypes.size());

        OrgType orgF001 = repositoryService.getObject(OrgType.class, ORG_F001_OID, null, opResult).asObjectable();
        AssertJUnit.assertNotNull(orgF001);
        AssertJUnit.assertEquals("F0001", orgF001.getName().getOrig());
        AssertJUnit.assertEquals("The office of the most respectful Governor.", orgF001.getDescription());

//		PrismAsserts.assertEqualsPolyString("Governor Office", "Governor Office", orgF001.getDisplayName());
        AssertJUnit.assertEquals("0001", orgF001.getIdentifier());
        AssertJUnit.assertEquals(1, orgF001.getOrgType().size());
        AssertJUnit.assertEquals("functional", orgF001.getOrgType().get(0));
        AssertJUnit.assertEquals("CC0", orgF001.getCostCenter());
//		PrismAsserts.assertEqualsPolyString("The Governor's Mansion", "The Governor's Mansion", orgF001.getLocality());


        OrgType orgF003 = repositoryService.getObject(OrgType.class, ORG_F003_OID, null, opResult).asObjectable();
        OrgType orgF004 = repositoryService.getObject(OrgType.class, ORG_F004_OID, null, opResult).asObjectable();
        OrgType orgF006 = repositoryService.getObject(OrgType.class, ORG_F006_OID, null, opResult).asObjectable();

        AssertJUnit.assertNotNull(orgF006);
        AssertJUnit.assertEquals("F0006", orgF006.getName().getOrig());
        AssertJUnit.assertEquals("Hosting the worst scumm of the Caribbean.", orgF006.getDescription());
        AssertJUnit.assertEquals("Scumm Bar", orgF006.getDisplayName().getOrig());
        AssertJUnit.assertEquals("0006", orgF006.getIdentifier());
        AssertJUnit.assertEquals(1, orgF006.getOrgType().size());
        AssertJUnit.assertEquals("functional", orgF006.getOrgType().get(0));
        AssertJUnit.assertEquals("Mêlée Island", orgF006.getLocality().getOrig());


        ObjectReferenceType oRefType;
        boolean isEqual003 = false;
        boolean isEqual004 = false;
        for (int i = 0; i < orgF006.getParentOrgRef().size(); i++) {
            oRefType = orgF006.getParentOrgRef().get(i);
            isEqual003 = isEqual003 || oRefType.getOid().equals(orgF003.getOid());
            isEqual004 = isEqual004 || oRefType.getOid().equals(orgF004.getOid());
        }

        AssertJUnit.assertEquals(true, isEqual003);
        AssertJUnit.assertEquals(true, isEqual004);


        OrgType pRoot = repositoryService.getObject(OrgType.class, ORG_PROJECT_ROOT_OID, null, opResult).asObjectable();
        AssertJUnit.assertEquals("PRoot", pRoot.getName().getOrig());
        AssertJUnit.assertEquals("Project organizational structure root", pRoot.getDescription());
//		PrismAsserts.assertEqualsPolyString("Projects", "Projects", pRoot.getDisplayName());
        AssertJUnit.assertEquals(1, pRoot.getOrgType().size());
        AssertJUnit.assertEquals("project", pRoot.getOrgType().get(0));

//		QueryType query = QueryUtil.createNameQuery(ELAINE_NAME);
        ObjectQuery query = new ObjectQuery();
        PrismObjectDefinition<UserType> userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        query.setFilter(EqualsFilter.createEqual(UserType.F_NAME, userObjectDef.findPropertyDefinition(UserType.F_NAME), null, ELAINE_NAME));

        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

        AssertJUnit.assertNotNull(users);
        AssertJUnit.assertEquals(1, users.size());
        UserType elaine = users.get(0).asObjectable();
        LOGGER.info("--->elaine<----");
//        LOGGER.info(prismContext.silentMarshalObject(elaine, LOGGER));
        AssertJUnit.assertEquals("Expected name elaine, but got " + elaine.getName().getOrig(), "elaine", elaine.getName().getOrig());
        AssertJUnit.assertEquals("Expected elaine has one org ref, but got " + elaine.getParentOrgRef().size(), 2, elaine.getParentOrgRef().size());
        AssertJUnit.assertEquals("Parent org ref oid not equal.", "00000000-8888-6666-0000-100000000001", elaine.getParentOrgRef().get(0).getOid());
//		AssertJUnit.assertEquals("Elaine is not manager, but: "
//				+ elaine.getParentOrgRef().get(0).getRelation().getLocalPart(), "manager", elaine.getParentOrgRef()
//				.get(0).getRelation().getLocalPart());
//		PrismAsserts.assertEqualsPolyString("Elaine Marley", "Elaine Marley", elaine.getFullName());
//		PrismAsserts.assertEqualsPolyString("Marley", "Marley", elaine.getFamilyName());
//		PrismAsserts.assertEqualsPolyString("Elaine", "Elaine", elaine.getGivenName());
//		PrismAsserts.assertEqualsPolyString("Governor", "Governor", elaine.getTitle());
        ELAINE_OID = elaine.getOid();

        // MID-1828
        testMonkeyMatch();

        LOGGER.info("==>after add<==");
        Session session = getFactory().openSession();
        session.beginTransaction();
        Query qCount = session.createQuery("select count(*) from ROrgClosure");
        long count = (Long) qCount.uniqueResult();
        AssertJUnit.assertEquals(54L, count);

        // check descendants for F0001 org unit
        /*Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("ancestor", "anc")
                .setFetchMode("ancestor", FetchMode.JOIN).
                add(Restrictions.eq("anc.oid", ORG_F001_OID));

        List<ROrgClosure> results = criteria.list();
        AssertJUnit.assertEquals(13, results.size());
        */
        // check descendants for F0001 org unit
        qCount = session.createQuery("select count(*) from ROrgClosure where ancestorOid = :ancestorOid");
        qCount.setParameter("ancestorOid", ORG_F001_OID);
        count = (Long) qCount.uniqueResult();
        AssertJUnit.assertEquals(13L, count);
        
        
        /*
        criteria = session.createCriteria(ROrgClosure.class)
        		.add(Restrictions.eq("depth", 2));
        Criteria ancCriteria = criteria.createCriteria("ancestor", "anc")
        		.setFetchMode("ancestor", FetchMode.JOIN)
                .add(Restrictions.eq("anc.oid", ORG_F001_OID));
        Criteria descCriteria = criteria.createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", ORG_F006_OID));
        results = criteria.list();
        AssertJUnit.assertEquals(1, results.size());
        */

        qCount = session.createQuery("select count(*) from ROrgClosure where ancestorOid = :ancestorOid and descendantOid = :descendantOid");
        qCount.setParameter("ancestorOid", ORG_F001_OID);
        qCount.setParameter("descendantOid", ORG_F006_OID);
        count = (Long) qCount.uniqueResult();
        AssertJUnit.assertEquals(1, count);

        session.getTransaction().commit();
        session.close();
    }

    /**
     * Tests for repo.matchObject() method
     */
    private void testMonkeyMatch() throws SchemaException, ObjectNotFoundException {
    	OperationResult result = new OperationResult(OrgStructTest.class.getName()+".testMonkeyMatch");
    	PrismObject<OrgType> org003 = repositoryService.getObject(OrgType.class, ORG_F003_OID, null, result);
    	PrismObject<OrgType> org001 = repositoryService.getObject(OrgType.class, ORG_F001_OID, null, result);
    	PrismObject<OrgType> org005 = repositoryService.getObject(OrgType.class, ORG_F005_OID, null, result);
    	
    	// properties
		assertMatch(org003, createOrgNameFilter("F0003"), true);
		assertMatch(org003, createOrgNameFilter("NoNSenSe"), false);
		
		// AND between properties
		assertMatch(org003, 
				AndFilter.createAnd(
						createOrgNameFilter("F0003"),
						createOrgDisplayNameFilter("Ministry of Offense")),
				true);
		
		assertMatch(org003, 
				AndFilter.createAnd(
						createOrgNameFilter("007"),
						createOrgDisplayNameFilter("Ministry of Offense")),
				false);
		
		assertMatch(org003, 
				AndFilter.createAnd(
						createOrgNameFilter("F0003"),
						createOrgDisplayNameFilter("Departmemt of NONsenSE")),
				false);
		
		// Orgs
		assertMatch(org003, OrgFilter.createOrg(ORG_F001_OID), true);
		assertMatch(org003, OrgFilter.createOrg(ORG_F003_OID), true);
		assertMatch(org003, OrgFilter.createOrg(ORG_F005_OID), false);
		assertMatch(org003, OrgFilter.createOrg(ORG_F002_OID), false);
		
		assertMatch(org001, OrgFilter.createOrg(ORG_F001_OID), true);
		assertMatch(org003, OrgFilter.createOrg(ORG_F001_OID), true);
		assertMatch(org005, OrgFilter.createOrg(ORG_F001_OID), true);
		
		// OR betweens orgs
		assertMatch(org005, 
				OrFilter.createOr(
						OrgFilter.createOrg(ORG_F002_OID),
						OrgFilter.createOrg(ORG_F003_OID)),
				true);
		assertMatch(org005, 
				OrFilter.createOr(
						OrgFilter.createOrg(ORG_F002_OID),
						OrgFilter.createOrg(ORG_F004_OID)),
				false);

		// org AND property 
		assertMatch(org003, 
				AndFilter.createAnd(
						OrgFilter.createOrg(ORG_F001_OID),
						createOrgNameFilter("F0003")),
				true);

		assertMatch(org003, 
				AndFilter.createAnd(
						OrgFilter.createOrg(ORG_F001_OID),
						createOrgNameFilter("blabla")),
				false);

		assertMatch(org003, 
				AndFilter.createAnd(
						OrgFilter.createOrg(ORG_F005_OID),
						createOrgNameFilter("F0003")),
				false);
		
		// (OR between orgs) AND property
		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F003_OID)),
					createOrgNameFilter("F0005")),
				true);

		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F003_OID)),
					createOrgNameFilter("blurbrrfrr!")),
				false);
		
		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F004_OID)),
					createOrgNameFilter("F0005")),
				false);
		
		// (OR between orgs) AND (AND between properties)
		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F003_OID)),
					AndFilter.createAnd(
							createOrgNameFilter("F0005"),
							createOrgDisplayNameFilter("Swashbuckler Section"))),
				true);
		
		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F004_OID)),
					AndFilter.createAnd(
							createOrgNameFilter("F0005"),
							createOrgDisplayNameFilter("Swashbuckler Section"))),
				false);
		
		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F003_OID)),
					AndFilter.createAnd(
							createOrgNameFilter("007"),
							createOrgDisplayNameFilter("Swashbuckler Section"))),
				false);
		
		assertMatch(org005,
				AndFilter.createAnd(
					OrFilter.createOr(
							OrgFilter.createOrg(ORG_F002_OID),
							OrgFilter.createOrg(ORG_F003_OID)),
					AndFilter.createAnd(
							createOrgNameFilter("F0005"),
							createOrgDisplayNameFilter("landLUBER subSECtion"))),
				false);
}
    
    private ObjectFilter createOrgNameFilter(String name) throws SchemaException {
		return EqualsFilter.createEqual(OrgType.F_NAME, OrgType.class, prismContext, PrismTestUtil.createPolyString(name));
	}
    
    private ObjectFilter createOrgDisplayNameFilter(String name) throws SchemaException {
		return EqualsFilter.createEqual(OrgType.F_DISPLAY_NAME, OrgType.class, prismContext, PrismTestUtil.createPolyString(name));
	}

	private <O extends ObjectType> void assertMatch(PrismObject<O> obj, ObjectFilter filter, boolean expected) throws SchemaException {
    	ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		boolean actual = repositoryService.matchObject(obj, query);
		assertEquals("Wrong match for "+obj+" and "+filter, expected, actual);
    }

	@SuppressWarnings({"unchecked"})
    @Test
    public void test001addOrgStructObjectsIncorrect() throws Exception {

    	LOGGER.info("===[ addIncorrectOrgStruct ]===");
    	
         OperationResult opResult = new OperationResult("===[ addIncorrectOrgStruct ]===");       
                  
        List<PrismObject<? extends Objectable>> orgStructIncorrect = prismContext.parseObjects(
        	 new File(ORG_STRUCT_OBJECTS_INCORRECT));
            
        for (PrismObject<? extends Objectable> o : orgStructIncorrect) {  	 
         repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }

        OrgType orgF008 = repositoryService.getObject(OrgType.class, ORG_F008_OID, null, opResult).asObjectable();
        OrgType orgF009 = repositoryService.getObject(OrgType.class, ORG_F009_OID, null, opResult).asObjectable();
        AssertJUnit.assertEquals(orgF008.getParentOrgRef().get(0).getOid(), orgF009.getOid());

        OrgType orgF010 = repositoryService.getObject(OrgType.class, ORG_F010_OID, null, opResult).asObjectable();

        ObjectReferenceType oRefType = new ObjectReferenceType();
        boolean isEqual009 = false;
        boolean isEqual012 = false;
        for (int i = 0; i < orgF010.getParentOrgRef().size(); i++) {
            oRefType = orgF010.getParentOrgRef().get(i);
            isEqual009 = isEqual009 || oRefType.getOid().equals(orgF009.getOid());
            isEqual012 = isEqual012 || oRefType.getOid().equals(ORG_F012_OID);
        }

        AssertJUnit.assertEquals(true, isEqual009);
        AssertJUnit.assertEquals(true, isEqual012);

        LOGGER.info("==>after add<==");
        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==============CLOSURE TABLE==========");
        List<ROrgClosure> orgClosure = session.createQuery("from ROrgClosure").list();
        // descendants of F007 - F007<0>, F009<1>, F008<2>, F0010<2>
        Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("ancestor", "anc")
                .setFetchMode("ancestor", FetchMode.JOIN)
                .add(Restrictions.eq("anc.oid", ORG_F007_OID));

        orgClosure = criteria.list();
        AssertJUnit.assertEquals(5, orgClosure.size());

        criteria = session.createCriteria(ROrgClosure.class)
                .add(Restrictions.eq("depth", 2))
                .createCriteria("ancestor", "anc")
                .setFetchMode("ancestor", FetchMode.JOIN)
                .add(Restrictions.eq("anc.oid", ORG_F007_OID));


        orgClosure = criteria.list();
        AssertJUnit.assertEquals(3, orgClosure.size());

        LOGGER.info("==============ORG INCORRECT TABLE==========");
        List<ROrgIncorrect> orgIncorrect = session.createQuery("from ROrgIncorrect").list();
        AssertJUnit.assertEquals(1, orgIncorrect.size());
        AssertJUnit.assertEquals(ORG_F012_OID, orgIncorrect.get(0).getAncestorOid());
        AssertJUnit.assertEquals(ORG_F010_OID, orgIncorrect.get(0).getDescendantOid());


        ObjectQuery query = new ObjectQuery();
        PrismObjectDefinition<UserType> userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        query.setFilter(EqualsFilter.createEqual(UserType.F_NAME, userObjectDef.findPropertyDefinition(UserType.F_NAME), null, ELAINE_NAME1));

        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

        AssertJUnit.assertNotNull(users);
        AssertJUnit.assertEquals(1, users.size());
        UserType elaine1 = users.get(0).asObjectable();
        LOGGER.info("--->elaine1<----");
//        LOGGER.info(prismContext.silentMarshalObject(elaine1, LOGGER));
        AssertJUnit.assertEquals("Expected name elaine, but got " + elaine1.getName().getOrig(), "elaine1", elaine1.getName().getOrig());
        AssertJUnit.assertEquals("Expected elaine has one org ref, but got " + elaine1.getParentOrgRef().size(), 2, elaine1.getParentOrgRef().size());
        AssertJUnit.assertEquals("Parent org ref oid not equal.", "00000000-8888-6666-0000-100000000011", elaine1.getParentOrgRef().get(0).getOid());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test002modifyOrgStructAddRef() throws Exception {
        LOGGER.info("===[ modifyOrgStruct ]===");
        OperationResult opResult = new OperationResult("===[ modifyOrgStruct ]===");
        // test modification of org ref in another org type..

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_ADD_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();

        Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", MODIFY_ORG_ADD_REF_OID));

        List<ROrgClosure> orgClosure = criteria.list();

        LOGGER.info("before modify");
        for (ROrgClosure c : orgClosure) {
            LOGGER.info("{}\t{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid(), c.getDepth()});
        }
        AssertJUnit.assertEquals(3, orgClosure.size());
        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_ADD_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();
        criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", MODIFY_ORG_ADD_REF_OID));

        orgClosure = criteria.list();

        LOGGER.info("after modify");
        for (ROrgClosure c : orgClosure) {
            LOGGER.info("{}\t{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid(), c.getDepth()});
        }
        AssertJUnit.assertEquals(4, orgClosure.size());

        List<String> ancestors = new ArrayList<String>();
        ancestors.add(MODIFY_ORG_ADD_REF_OID);
        ancestors.add(ORG_F003_OID);
        ancestors.add(ORG_F001_OID);
        ancestors.add(ORG_F002_OID);

        for (String ancestorOid : ancestors) {
            criteria = session.createCriteria(ROrgClosure.class);
            criteria.createCriteria("ancestor", "anc").setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ancestorOid));
            criteria.createCriteria("descendant", "desc")
                    .setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", MODIFY_ORG_ADD_REF_OID));

            orgClosure = criteria.list();

            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{orgClosure.get(0).getAncestor().toJAXB(prismContext, null),
                    orgClosure.get(0).getDescendant().toJAXB(prismContext, null), orgClosure.get(0).getDepth()});

//			LOGGER.info("==============CLOSURE TABLE FOR EACH==========");
//			
//			for (ROrgClosure o : results) {
//				LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
//						o.getDescendant().toJAXB(prismContext), o.getDepth() });
//
//			}

            AssertJUnit.assertEquals(1, orgClosure.size());
            AssertJUnit.assertEquals(ancestorOid, orgClosure.get(0)
                    .getAncestor().getOid());
            AssertJUnit.assertEquals(MODIFY_ORG_ADD_REF_OID, orgClosure.get(0)
                    .getDescendant().getOid());
            int depth = -1;
            if (ancestorOid.equals(MODIFY_ORG_ADD_REF_OID)) {
                depth = 0;
            } else if (ancestorOid.equals(ORG_F001_OID)) {
                depth = 2;
            } else if (ancestorOid.equals(ORG_F003_OID)) {
                depth = 1;
            } else if (ancestorOid.equals(ORG_F002_OID)) {
                depth = 1;
            }
            AssertJUnit.assertEquals(depth, orgClosure.get(0).getDepth());

        }
        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test002modifyOrgStructAddRefIncorrect() throws Exception {
        LOGGER.info("===[ modifyOrgStruct ]===");
        OperationResult opResult = new OperationResult("===[ modifyOrgStructIncorrect ]===");
        // test modification of org ref in another org type..

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_INCORRECT_ADD_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_INCORRECT_ADD_REF_OID, delta.getModifications(), opResult);

        Session session = getFactory().openSession();
        session.beginTransaction();

        Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", MODIFY_ORG_INCORRECT_ADD_REF_OID));

        List<ROrgClosure> orgClosure = criteria.list();

        LOGGER.info("after modify incorrect - closure");
        for (ROrgClosure c : orgClosure) {
            LOGGER.info("{}\t{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid(), c.getDepth()});
        }
        AssertJUnit.assertEquals(5, orgClosure.size());

        List<String> ancestors = new ArrayList<String>();
        ancestors.add(MODIFY_ORG_INCORRECT_ADD_REF_OID);
        ancestors.add(ORG_F001_OID);
        ancestors.add(ORG_F002_OID);

        for (String ancestorOid : ancestors) {
            criteria = session.createCriteria(ROrgClosure.class);
            criteria.createCriteria("ancestor", "anc").setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ancestorOid));
            criteria.createCriteria("descendant", "desc")
                    .setFetchMode("descendant", FetchMode.JOIN)
                    .add(Restrictions.eq("desc.oid", MODIFY_ORG_INCORRECT_ADD_REF_OID));

            orgClosure = criteria.list();

            AssertJUnit.assertEquals(1, orgClosure.size());

            AssertJUnit.assertEquals(ancestorOid, orgClosure.get(0).getAncestor().getOid());
            AssertJUnit.assertEquals(MODIFY_ORG_INCORRECT_ADD_REF_OID, orgClosure.get(0).getDescendant().getOid());

            int depth = -1;
            if (ancestorOid.equals(MODIFY_ORG_INCORRECT_ADD_REF_OID)) {
                depth = 0;
            } else if (ancestorOid.equals(ORG_F001_OID)) {
                depth = 2;
            } else if (ancestorOid.equals(ORG_F002_OID)) {
                depth = 1;
            }
            AssertJUnit.assertEquals(depth, orgClosure.get(0).getDepth());
        }


        criteria = session.createCriteria(ROrgIncorrect.class)
                .add(Restrictions.eq("descendantOid", MODIFY_ORG_INCORRECT_ADD_REF_OID));

        List<ROrgIncorrect> orgIncorrect = criteria.list();

        LOGGER.info("after modify incorrect - incorrect");
        for (ROrgIncorrect c : orgIncorrect) {
            LOGGER.info("{}\t{}\t{}", new Object[]{c.getAncestorOid(), c.getDescendantOid()});
        }
        AssertJUnit.assertEquals(1, orgIncorrect.size());
        AssertJUnit.assertEquals(MODIFY_ORG_INCORRECT_ADD_REF_OID, orgIncorrect.get(0).getDescendantOid());
        AssertJUnit.assertEquals(ORG_F012_OID, orgIncorrect.get(0).getAncestorOid());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test003modifyOrgStructDeleteRef() throws Exception {

        // test modification of org ref - delete org ref
        LOGGER.info("===[ modify delete org ref ]===");
        OperationResult opResult = new OperationResult("===[ modify delete org ref ]===");
        
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_DELETE_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==>before modify - delete<==");

        Query qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid and o.ancestorOid = :ancestorOid and o.depth = :depth");
        qDelete.setParameter("descendantOid", MODIFY_ORG_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F003_OID);
        qDelete.setParameter("depth", 1);

        List<ROrgClosure> orgClosure = qDelete.list();

        AssertJUnit.assertEquals(1, orgClosure.size());

        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_DELETE_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();

        LOGGER.info("==>after modify - delete<==");

        qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid  and o.ancestorOid = :ancestorOid and o.depth = :depth");
        qDelete.setParameter("descendantOid", MODIFY_ORG_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F003_OID);
        qDelete.setParameter("depth", 1);

        orgClosure = qDelete.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test003modifyOrgStructDeleteRefIncorrect() throws Exception {

        // test modification of org ref - delete org ref
        LOGGER.info("===[ modify delete org ref ]===");
        OperationResult opResult = new OperationResult("===[ modify delete org ref ]===");
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_INCORRECT_DELETE_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==>before modify - delete<==");

        Query qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid  and o.ancestorOid = :ancestorOid");
        qDelete.setParameter("descendantOid", MODIFY_ORG_INCORRECT_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F012_OID);

        List<ROrgClosure> orgClosure = qDelete.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_INCORRECT_DELETE_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();

        LOGGER.info("==>after modify - delete<==");

        qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid and o.ancestorOid = :ancestorOid");
        qDelete.setParameter("descendantOid", MODIFY_ORG_INCORRECT_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F012_OID);

        orgClosure = qDelete.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test004modifyOrgStructAddUser() throws Exception {
        LOGGER.info("===[ modify add user to orgStruct ]===");

        Session session = getFactory().openSession();
        
        /* session.beginTransaction();
        
        Query sqlUser = session.createQuery("from RUser where givenName_norm = :userName");
        sqlUser.setParameter("userName", ELAINE_NAME);
        List<RUser> users = sqlUser.list();
        
        AssertJUnit.assertEquals(1,users.size());
        
        ELAINE_OID = users.get(0).getOid();

        session.getTransaction().commit();
        */

        OperationResult opResult = new OperationResult("===[ modify add user to orgStruct ]===");

        //test modification of org ref in another org type..
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_ADD_USER_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

        //session.clear();
        session.beginTransaction();

        LOGGER.info("==>after modify - add user to org<==");

        Query sqlOrgClosure = session.createQuery("from ROrgClosure as o where o.descendantOid = :descendantOid");
        sqlOrgClosure.setParameter("descendantOid", ELAINE_OID);
        List<ROrgClosure> orgClosure = sqlOrgClosure.list();

        AssertJUnit.assertEquals(7, orgClosure.size());

        LOGGER.info("==============CLOSURE TABLE==========");

        for (ROrgClosure o : orgClosure) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext, null),
                    o.getDescendant().toJAXB(prismContext, null), o.getDepth()});

        }
        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test005deleteOrg() throws Exception {
        LOGGER.info("===[ deleteOrgStruct ]===");

        OperationResult opResult = new OperationResult("===[ deleteOrgStruct ]===");

        repositoryService.deleteObject(OrgType.class, DELETE_ORG_OID, opResult);

        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==>after delete<==");

        Query sqlOrgClosure = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :deleteOid or o.ancestorOid = :deleteOid");
        sqlOrgClosure.setParameter("deleteOid", DELETE_ORG_OID);

        List<ROrgClosure> orgClosure = sqlOrgClosure.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        LOGGER.info("==============CLOSURE TABLE==========");

        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test006searchOrgStructUserUnbounded() throws Exception {

        LOGGER.info("===[ SEARCH QUERY ]===");
        OperationResult parentResult = new OperationResult("search objects - org struct unbound");


        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(SEARCH_ORG_OID_UNBOUNDED_DEPTH));
        objectQuery.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        List<PrismObject<ObjectType>> orgClosure = repositoryService.searchObjects(ObjectType.class, objectQuery, null, parentResult);

        for (PrismObject<ObjectType> u : orgClosure) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }

        AssertJUnit.assertEquals(8, orgClosure.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test007searchOrgStructOrgDepth() throws Exception {
        LOGGER.info("===[ SEARCH QUERY ]===");
        OperationResult parentResult = new OperationResult("search objects - org struct");

        Session session = getFactory().openSession();
        session.beginTransaction();

        Query sqlSearch = session.createQuery("from ROrgClosure where depthValue = 1 and " +
                "(ancestor_oid = :searchOid or descendant_oid = :searchOid)");
        sqlSearch.setParameter("searchOid", SEARCH_ORG_OID_DEPTH1);

        List<ROrgClosure> orgClosure = sqlSearch.list();

        AssertJUnit.assertEquals(4, orgClosure.size());

        LOGGER.info("==============CLOSURE TABLE==========");
        for (ROrgClosure o : orgClosure) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext, null),
                    o.getDescendant().toJAXB(prismContext, null), o.getDepth()});
        }
        session.getTransaction().commit();
        session.close();


        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(SEARCH_ORG_OID_DEPTH1, null, 1));
        objectQuery.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        List<PrismObject<ObjectType>> sOrgClosure = repositoryService.searchObjects(ObjectType.class, objectQuery, null, parentResult);

        AssertJUnit.assertEquals(5, sOrgClosure.size());

        for (PrismObject<ObjectType> u : sOrgClosure) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }
    }

    @Test
    public void test008searchRootOrg() throws Exception {
        LOGGER.info("===[ SEARCH ROOT QUERY ]===");
        OperationResult parentResult = new OperationResult("search root org struct");
         /*       
        Session session = getFactory().openSession();
        session.beginTransaction();
       
        Query sqlSearch = session.createQuery("select distinct ancestor_oid from ROrgClosure where ancestor_oid " +
        		" not in (select distinct descendant_oid from ROrgClosure where depthValue > 0)");
        List<ROrgClosure> orgClosure = sqlSearch.list();

        AssertJUnit.assertEquals(5, orgClosure.size());
        
        for (ROrgClosure o : orgClosure) 
        {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
                    o.getDescendant().toJAXB(prismContext), o.getDepth()});
        }

        session.getTransaction().commit();
        session.close();
*/
        ObjectQuery qSearch = ObjectQuery.createObjectQuery(OrgFilter.createRootOrg());
        qSearch.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        List<PrismObject<OrgType>> rootOrgs = repositoryService.searchObjects(OrgType.class, qSearch, null, parentResult);

        for (PrismObject<OrgType> ro : rootOrgs) {
            LOGGER.info("ROOT  ========= {}", ObjectTypeUtil.toShortString(ro.asObjectable()));
        }
        AssertJUnit.assertEquals(5, rootOrgs.size());
    }


    @Test
    public void test009modifyOrgStructRemoveUser() throws Exception {
        LOGGER.info("===[ modify remove org ref from user ]===");

        OperationResult opResult = new OperationResult("===[ modify remove org ref from user ]===");

        PrismReferenceValue prv = new PrismReferenceValue(MODIFY_USER_DELETE_REF_OID);
        prv.setTargetType(OrgType.COMPLEX_TYPE);
        ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteReference(UserType.class, ELAINE_OID, UserType.F_PARENT_ORG_REF, prismContext, prv);

        repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

        UserType userElaine = repositoryService.getObject(UserType.class, ELAINE_OID, null, opResult).asObjectable();
        LOGGER.trace("elaine's og refs");
        for (ObjectReferenceType ort : userElaine.getParentOrgRef()) {
            LOGGER.trace("{}", ort);
            if (ort.getOid().equals(MODIFY_USER_DELETE_REF_OID)) {
                AssertJUnit.fail("expected that elain does not have reference on the org with oid:" + MODIFY_USER_DELETE_REF_OID);
            }
        }
    }

    @Test
    public void test010megaSimpleTest() throws Exception {
        LOGGER.info("===[test010megaSimpleTest]===");
        try {
            RUser user = new RUser();
            user.setOid(UUID.randomUUID().toString());
            user.setName(new RPolyString("vilko", "vilko"));
            Set<RObjectReference> refs = new HashSet<RObjectReference>();
            refs.add(createRef(user, "1", null, null));
            refs.add(createRef(user, "1", "namespace", "localpart"));
            refs.add(createRef(user, "6", null, null));
            user.setParentOrgRef(refs);

            Session session = getFactory().openSession();
            session.beginTransaction();
            String id = (String) session.save(user);
            session.getTransaction().commit();
            session.close();

            user = getUser(id, 3, user);
            //todo asserts

            user = new RUser();
            user.setOid(id);
            user.setName(new RPolyString("vilko", "vilko"));
            refs = new HashSet<>();
            refs.add(createRef(user, "1", null, null));
            refs.add(createRef(user, "1", "namespace", "localpart"));
            user.setParentOrgRef(refs);

            session = getFactory().openSession();
            session.beginTransaction();
            session.merge(user);
            session.getTransaction().commit();
            session.close();

            user = getUser(id, 2, user);
            //todo asserts
        } catch (Exception ex) {
            LOGGER.error("Exception occurred.", ex);
            throw ex;
        }
    }

    @Test
    public void test011OrgFilter() throws Exception {
        LOGGER.info("===[ test sorting org filter ]===");

        OperationResult opResult = new OperationResult("===[ test sorting org filter  ]===");

        ObjectQuery query = new ObjectQuery();
        PrismReferenceValue baseOrgRef = new PrismReferenceValue(ORG_F001_OID);
        ObjectFilter filter = OrgFilter.createOrg(baseOrgRef, null, 1);
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOrderBy(ObjectType.F_NAME);
        query.setFilter(filter);
        query.setPaging(paging);

        List<PrismObject<ObjectType>> orgClosure = repositoryService.searchObjects(ObjectType.class, query, null, opResult);

        AssertJUnit.assertEquals(5, orgClosure.size());

        for (PrismObject<ObjectType> u : orgClosure) {
            LOGGER.info("CHILD ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }
    }

    private RUser getUser(String oid, int count, RUser other) {
        Session session = getFactory().openSession();
        session.beginTransaction();
        RUser user = (RUser) session.get(RUser.class, oid);
        AssertJUnit.assertEquals(count, user.getParentOrgRef().size());

        session.getTransaction().commit();
        session.close();

        return user;
    }

    private RObjectReference createRef(RUser user, String targetOid, String namespace, String localpart) {
        RParentOrgRef ref = new RParentOrgRef();
        ref.setOwner(user);
        ref.setTargetOid(targetOid);
        ref.setType(RObjectType.ORG);

        QName q = null;
        if (namespace != null && localpart != null) {
            q = new QName(namespace, localpart);
        }
        ref.setRelation(RUtil.qnameToString(q));

        return ref;
    }
}
