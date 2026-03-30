package com.vn.core.web.rest;

import static com.vn.core.domain.ShoeAsserts.*;
import static com.vn.core.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.domain.Authority;
import com.vn.core.domain.Shoe;
import com.vn.core.domain.RoleType;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.repository.ShoeRepository;
import com.vn.core.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ShoeResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "shoe-user", authorities = "ROLE_SHOE_WORKBENCH")
class ShoeResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DECRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DECRIPTION = "BBBBBBBBBB";

    private static final String AUTHORITY_NAME = "ROLE_SHOE_WORKBENCH";
    private static final String ENTITY_API_URL = "/api/shoes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String PERMISSION_ADMIN_API_URL = "/api/admin/sec/permissions";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ShoeRepository shoeRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restShoeMockMvc;

    private Shoe shoe;

    private Shoe insertedShoe;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Shoe createEntity() {
        return new Shoe().name(DEFAULT_NAME).decription(DEFAULT_DECRIPTION);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Shoe createUpdatedEntity() {
        return new Shoe().name(UPDATED_NAME).decription(UPDATED_DECRIPTION);
    }

    @BeforeEach
    void initTest() throws Exception {
        shoe = createEntity();
        grantShoeCrudPermissions(AUTHORITY_NAME);
    }

    @AfterEach
    void cleanup() {
        if (insertedShoe != null) {
            shoeRepository.delete(insertedShoe);
            insertedShoe = null;
        }
    }

    @Test
    @Transactional
    void createShoe() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Shoe
        var returnedShoe = om.readValue(
            restShoeMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoe)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Shoe.class
        );

        // Validate the Shoe in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertShoeUpdatableFieldsEquals(returnedShoe, getPersistedShoe(returnedShoe));

        insertedShoe = returnedShoe;
    }

    @Test
    @Transactional
    void createShoeWithExistingId() throws Exception {
        // Create the Shoe with an existing ID
        shoe.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restShoeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoe)))
            .andExpect(status().isBadRequest());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllShoes() throws Exception {
        // Initialize the database
        insertedShoe = shoeRepository.saveAndFlush(shoe);

        // Get all the shoeList
        restShoeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shoe.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].decription").value(hasItem(DEFAULT_DECRIPTION)));
    }

    @Test
    @Transactional
    void getShoe() throws Exception {
        // Initialize the database
        insertedShoe = shoeRepository.saveAndFlush(shoe);

        // Get the shoe
        restShoeMockMvc
            .perform(get(ENTITY_API_URL_ID, shoe.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(shoe.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.decription").value(DEFAULT_DECRIPTION));
    }

    @Test
    @Transactional
    void getNonExistingShoe() throws Exception {
        // Get the shoe
        restShoeMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingShoe() throws Exception {
        // Initialize the database
        insertedShoe = shoeRepository.saveAndFlush(shoe);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shoe
        Shoe updatedShoe = shoeRepository.findById(shoe.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedShoe are not directly saved in db
        em.detach(updatedShoe);
        updatedShoe.name(UPDATED_NAME).decription(UPDATED_DECRIPTION);

        restShoeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedShoe.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedShoe))
            )
            .andExpect(status().isOk());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedShoeToMatchAllProperties(updatedShoe);
    }

    @Test
    @Transactional
    void putNonExistingShoe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoe.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShoeMockMvc
            .perform(put(ENTITY_API_URL_ID, shoe.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoe)))
            .andExpect(status().isBadRequest());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchShoe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(shoe))
            )
            .andExpect(status().isBadRequest());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamShoe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoe)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateShoeWithPatch() throws Exception {
        // Initialize the database
        insertedShoe = shoeRepository.saveAndFlush(shoe);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shoe using partial update
        Shoe partialUpdatedShoe = new Shoe();
        partialUpdatedShoe.setId(shoe.getId());

        partialUpdatedShoe.decription(UPDATED_DECRIPTION);

        restShoeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedShoe.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedShoe))
            )
            .andExpect(status().isOk());

        // Validate the Shoe in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertShoeUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedShoe, shoe), getPersistedShoe(shoe));
    }

    @Test
    @Transactional
    void fullUpdateShoeWithPatch() throws Exception {
        // Initialize the database
        insertedShoe = shoeRepository.saveAndFlush(shoe);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shoe using partial update
        Shoe partialUpdatedShoe = new Shoe();
        partialUpdatedShoe.setId(shoe.getId());

        partialUpdatedShoe.name(UPDATED_NAME).decription(UPDATED_DECRIPTION);

        restShoeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedShoe.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedShoe))
            )
            .andExpect(status().isOk());

        // Validate the Shoe in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertShoeUpdatableFieldsEquals(partialUpdatedShoe, getPersistedShoe(partialUpdatedShoe));
    }

    @Test
    @Transactional
    void patchNonExistingShoe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoe.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShoeMockMvc
            .perform(patch(ENTITY_API_URL_ID, shoe.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(shoe)))
            .andExpect(status().isBadRequest());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchShoe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(shoe))
            )
            .andExpect(status().isBadRequest());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamShoe() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoe.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(shoe)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Shoe in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteShoe() throws Exception {
        // Initialize the database
        insertedShoe = shoeRepository.saveAndFlush(shoe);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the shoe
        restShoeMockMvc
            .perform(delete(ENTITY_API_URL_ID, shoe.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return shoeRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Shoe getPersistedShoe(Shoe shoe) {
        return shoeRepository.findById(shoe.getId()).orElseThrow();
    }

    protected void assertPersistedShoeToMatchAllProperties(Shoe expectedShoe) {
        assertShoeAllPropertiesEquals(expectedShoe, getPersistedShoe(expectedShoe));
    }

    protected void assertPersistedShoeToMatchUpdatableProperties(Shoe expectedShoe) {
        assertShoeAllUpdatablePropertiesEquals(expectedShoe, getPersistedShoe(expectedShoe));
    }

    private void grantShoeCrudPermissions(String authorityName) throws Exception {
        grantEntityPermission(authorityName, "shoe", "READ");
        grantEntityPermission(authorityName, "shoe", "CREATE");
        grantEntityPermission(authorityName, "shoe", "UPDATE");
        grantEntityPermission(authorityName, "shoe", "DELETE");
        grantAttributePermission(authorityName, "shoe.name", "VIEW");
        grantAttributePermission(authorityName, "shoe.decription", "VIEW");
        grantAttributePermission(authorityName, "shoe.name", "EDIT");
        grantAttributePermission(authorityName, "shoe.decription", "EDIT");
    }

    private void grantEntityPermission(String authorityName, String target, String action) throws Exception {
        ensureAuthorityExists(authorityName);
        Map<String, Object> permissionPayload = Map.of(
            "authorityName",
            authorityName,
            "targetType",
            "ENTITY",
            "target",
            target,
            "action",
            action,
            "effect",
            "GRANT"
        );

        restShoeMockMvc
            .perform(
                post(PERMISSION_ADMIN_API_URL)
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(permissionPayload))
            )
            .andExpect(status().isCreated());
    }

    private void grantAttributePermission(String authorityName, String target, String action) throws Exception {
        ensureAuthorityExists(authorityName);
        Map<String, Object> permissionPayload = Map.of(
            "authorityName",
            authorityName,
            "targetType",
            "ATTRIBUTE",
            "target",
            target,
            "action",
            action,
            "effect",
            "GRANT"
        );

        restShoeMockMvc
            .perform(
                post(PERMISSION_ADMIN_API_URL)
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(permissionPayload))
            )
            .andExpect(status().isCreated());
    }

    private void ensureAuthorityExists(String authorityName) {
        if (authorityRepository.findById(authorityName).isPresent()) {
            return;
        }

        Authority authority = new Authority().name(authorityName).displayName(authorityName).type(RoleType.RESOURCE);
        authorityRepository.saveAndFlush(authority);
    }
}
