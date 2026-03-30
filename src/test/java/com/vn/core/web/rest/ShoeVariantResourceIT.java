package com.vn.core.web.rest;

import static com.vn.core.domain.ShoeVariantAsserts.*;
import static com.vn.core.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.domain.Authority;
import com.vn.core.domain.RoleType;
import com.vn.core.domain.ShoeVariant;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.repository.ShoeVariantRepository;
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
 * Integration tests for the {@link ShoeVariantResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "shoe-variant-user", authorities = "ROLE_SHOEVARIANT_WORKBENCH")
class ShoeVariantResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DECRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DECRIPTION = "BBBBBBBBBB";

    private static final String AUTHORITY_NAME = "ROLE_SHOEVARIANT_WORKBENCH";
    private static final String ENTITY_API_URL = "/api/shoe-variants";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String PERMISSION_ADMIN_API_URL = "/api/admin/sec/permissions";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ShoeVariantRepository shoeVariantRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restShoeVariantMockMvc;

    private ShoeVariant shoeVariant;

    private ShoeVariant insertedShoeVariant;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShoeVariant createEntity() {
        return new ShoeVariant().name(DEFAULT_NAME).decription(DEFAULT_DECRIPTION);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShoeVariant createUpdatedEntity() {
        return new ShoeVariant().name(UPDATED_NAME).decription(UPDATED_DECRIPTION);
    }

    @BeforeEach
    void initTest() throws Exception {
        shoeVariant = createEntity();
        grantShoeVariantCrudPermissions(AUTHORITY_NAME);
    }

    @AfterEach
    void cleanup() {
        if (insertedShoeVariant != null) {
            shoeVariantRepository.delete(insertedShoeVariant);
            insertedShoeVariant = null;
        }
    }

    @Test
    @Transactional
    void createShoeVariant() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ShoeVariant
        var returnedShoeVariant = om.readValue(
            restShoeVariantMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoeVariant)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ShoeVariant.class
        );

        // Validate the ShoeVariant in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertShoeVariantUpdatableFieldsEquals(returnedShoeVariant, getPersistedShoeVariant(returnedShoeVariant));

        insertedShoeVariant = returnedShoeVariant;
    }

    @Test
    @Transactional
    void createShoeVariantWithExistingId() throws Exception {
        // Create the ShoeVariant with an existing ID
        shoeVariant.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restShoeVariantMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoeVariant)))
            .andExpect(status().isBadRequest());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllShoeVariants() throws Exception {
        // Initialize the database
        insertedShoeVariant = shoeVariantRepository.saveAndFlush(shoeVariant);

        // Get all the shoeVariantList
        restShoeVariantMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shoeVariant.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].decription").value(hasItem(DEFAULT_DECRIPTION)));
    }

    @Test
    @Transactional
    void getShoeVariant() throws Exception {
        // Initialize the database
        insertedShoeVariant = shoeVariantRepository.saveAndFlush(shoeVariant);

        // Get the shoeVariant
        restShoeVariantMockMvc
            .perform(get(ENTITY_API_URL_ID, shoeVariant.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(shoeVariant.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.decription").value(DEFAULT_DECRIPTION));
    }

    @Test
    @Transactional
    void getNonExistingShoeVariant() throws Exception {
        // Get the shoeVariant
        restShoeVariantMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingShoeVariant() throws Exception {
        // Initialize the database
        insertedShoeVariant = shoeVariantRepository.saveAndFlush(shoeVariant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shoeVariant
        ShoeVariant updatedShoeVariant = shoeVariantRepository.findById(shoeVariant.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedShoeVariant are not directly saved in db
        em.detach(updatedShoeVariant);
        updatedShoeVariant.name(UPDATED_NAME).decription(UPDATED_DECRIPTION);

        restShoeVariantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedShoeVariant.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedShoeVariant))
            )
            .andExpect(status().isOk());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedShoeVariantToMatchAllProperties(updatedShoeVariant);
    }

    @Test
    @Transactional
    void putNonExistingShoeVariant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoeVariant.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShoeVariantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, shoeVariant.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(shoeVariant))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchShoeVariant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoeVariant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeVariantMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(shoeVariant))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamShoeVariant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoeVariant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeVariantMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shoeVariant)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateShoeVariantWithPatch() throws Exception {
        // Initialize the database
        insertedShoeVariant = shoeVariantRepository.saveAndFlush(shoeVariant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shoeVariant using partial update
        ShoeVariant partialUpdatedShoeVariant = new ShoeVariant();
        partialUpdatedShoeVariant.setId(shoeVariant.getId());

        restShoeVariantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedShoeVariant.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedShoeVariant))
            )
            .andExpect(status().isOk());

        // Validate the ShoeVariant in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertShoeVariantUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedShoeVariant, shoeVariant),
            getPersistedShoeVariant(shoeVariant)
        );
    }

    @Test
    @Transactional
    void fullUpdateShoeVariantWithPatch() throws Exception {
        // Initialize the database
        insertedShoeVariant = shoeVariantRepository.saveAndFlush(shoeVariant);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shoeVariant using partial update
        ShoeVariant partialUpdatedShoeVariant = new ShoeVariant();
        partialUpdatedShoeVariant.setId(shoeVariant.getId());

        partialUpdatedShoeVariant.name(UPDATED_NAME).decription(UPDATED_DECRIPTION);

        restShoeVariantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedShoeVariant.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedShoeVariant))
            )
            .andExpect(status().isOk());

        // Validate the ShoeVariant in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertShoeVariantUpdatableFieldsEquals(partialUpdatedShoeVariant, getPersistedShoeVariant(partialUpdatedShoeVariant));
    }

    @Test
    @Transactional
    void patchNonExistingShoeVariant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoeVariant.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShoeVariantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, shoeVariant.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(shoeVariant))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchShoeVariant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoeVariant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeVariantMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(shoeVariant))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamShoeVariant() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shoeVariant.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShoeVariantMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(shoeVariant)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ShoeVariant in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteShoeVariant() throws Exception {
        // Initialize the database
        insertedShoeVariant = shoeVariantRepository.saveAndFlush(shoeVariant);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the shoeVariant
        restShoeVariantMockMvc
            .perform(delete(ENTITY_API_URL_ID, shoeVariant.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return shoeVariantRepository.count();
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

    protected ShoeVariant getPersistedShoeVariant(ShoeVariant shoeVariant) {
        return shoeVariantRepository.findById(shoeVariant.getId()).orElseThrow();
    }

    protected void assertPersistedShoeVariantToMatchAllProperties(ShoeVariant expectedShoeVariant) {
        assertShoeVariantAllPropertiesEquals(expectedShoeVariant, getPersistedShoeVariant(expectedShoeVariant));
    }

    protected void assertPersistedShoeVariantToMatchUpdatableProperties(ShoeVariant expectedShoeVariant) {
        assertShoeVariantAllUpdatablePropertiesEquals(expectedShoeVariant, getPersistedShoeVariant(expectedShoeVariant));
    }

    private void grantShoeVariantCrudPermissions(String authorityName) throws Exception {
        grantEntityPermission(authorityName, "shoevariant", "READ");
        grantEntityPermission(authorityName, "shoevariant", "CREATE");
        grantEntityPermission(authorityName, "shoevariant", "UPDATE");
        grantEntityPermission(authorityName, "shoevariant", "DELETE");
        grantAttributePermission(authorityName, "shoevariant.name", "VIEW");
        grantAttributePermission(authorityName, "shoevariant.decription", "VIEW");
        grantAttributePermission(authorityName, "shoevariant.name", "EDIT");
        grantAttributePermission(authorityName, "shoevariant.decription", "EDIT");
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

        restShoeVariantMockMvc
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

        restShoeVariantMockMvc
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
