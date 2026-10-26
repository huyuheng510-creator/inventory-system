package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.AuthenticationException;
import com.cjlu.inventory.exception.EntityNotFoundException;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.dao.CategoryDao;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.dao.SupplierDao;
import com.cjlu.inventory.util.Validator;

import java.util.List;
import java.util.Optional;

/**
 * Category and supplier master-data use cases (FR-4, FR-5), including the
 * referential-integrity checks that refuse deletion while products reference
 * the record.
 */
public class CatalogService {

    private final CategoryDao CategoryDao;
    private final SupplierDao SupplierDao;
    private final ProductDao ProductDao;
    private final AuthService authService;

    public CatalogService(CategoryDao CategoryDao,
                          SupplierDao SupplierDao,
                          ProductDao ProductDao,
                          AuthService authService) {
        this.CategoryDao = CategoryDao;
        this.SupplierDao = SupplierDao;
        this.ProductDao = ProductDao;
        this.authService = authService;
    }

    // ---------------------------------------------------------------- categories

    public Category createCategory(String name, String description)
            throws ValidationException, AuthenticationException {
        authService.requireAdmin();
        String normalisedName = Validator.requireText(name, "Category name", 40);
        if (CategoryDao.findByName(normalisedName).isPresent()) {
            throw new ValidationException("Category '" + normalisedName + "' already exists.");
        }
        String normalisedDescription = Validator.optionalText(description, "Description", 120);
        Category category = new Category(normalisedName, normalisedDescription);
        CategoryDao.save(category);
        return category;
    }

    public void updateCategory(String id, String name, String description)
            throws EntityNotFoundException, ValidationException, AuthenticationException {
        authService.requireAdmin();
        Category category = requireCategory(id);
        String normalisedName = Validator.requireText(name, "Category name", 40);
        Optional<Category> sameName = CategoryDao.findByName(normalisedName);
        if (sameName.isPresent() && !sameName.get().getId().equals(category.getId())) {
            throw new ValidationException("Another category already uses the name '" + normalisedName + "'.");
        }
        category.setName(normalisedName);
        category.setDescription(Validator.optionalText(description, "Description", 120));
        CategoryDao.save(category);
    }

    public void deleteCategory(String id)
            throws EntityNotFoundException, InventoryException, AuthenticationException {
        authService.requireAdmin();
        Category category = requireCategory(id);
        int referencingProducts = ProductDao.findByCategoryId(category.getId()).size();
        if (referencingProducts > 0) {
            throw new InventoryException("Cannot delete category '" + category.getName()
                    + "': " + referencingProducts + " product(s) still use it.");
        }
        CategoryDao.deleteById(category.getId());
    }

    public List<Category> listCategories() {
        return CategoryDao.findAll();
    }

    public Category requireCategory(String id) throws EntityNotFoundException {
        if (id == null || id.isBlank()) {
            throw new EntityNotFoundException("Category not found.");
        }
        return CategoryDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
    }

    /** Human-readable category name for a product row, never fails. */
    public String describeCategory(String categoryId) {
        return CategoryDao.findById(categoryId)
                .map(Category::getName)
                .orElse("Uncategorised");
    }

    // ----------------------------------------------------------------- suppliers

    public Supplier createSupplier(String name, String contactName, String phone, String email)
            throws ValidationException, AuthenticationException {
        authService.requireAdmin();
        String normalisedName = Validator.requireText(name, "Supplier name", 60);
        if (SupplierDao.findByName(normalisedName).isPresent()) {
            throw new ValidationException("Supplier '" + normalisedName + "' already exists.");
        }
        String normalisedContact = Validator.requireText(contactName, "Contact name", 40);
        String normalisedPhone = Validator.requirePhone(phone);
        String normalisedEmail = Validator.requireEmail(email);
        Supplier supplier = new Supplier(normalisedName, normalisedContact,
                normalisedPhone, normalisedEmail);
        SupplierDao.save(supplier);
        return supplier;
    }

    public void updateSupplier(String id, String name, String contactName, String phone, String email)
            throws EntityNotFoundException, ValidationException, AuthenticationException {
        authService.requireAdmin();
        Supplier supplier = requireSupplier(id);
        String normalisedName = Validator.requireText(name, "Supplier name", 60);
        Optional<Supplier> sameName = SupplierDao.findByName(normalisedName);
        if (sameName.isPresent() && !sameName.get().getId().equals(supplier.getId())) {
            throw new ValidationException("Another supplier already uses the name '" + normalisedName + "'.");
        }
        supplier.setName(normalisedName);
        supplier.setContactName(Validator.requireText(contactName, "Contact name", 40));
        supplier.setPhone(Validator.requirePhone(phone));
        supplier.setEmail(Validator.requireEmail(email));
        SupplierDao.save(supplier);
    }

    public void deleteSupplier(String id)
            throws EntityNotFoundException, InventoryException, AuthenticationException {
        authService.requireAdmin();
        Supplier supplier = requireSupplier(id);
        long referencingProducts = ProductDao.findAll().stream()
                .filter(product -> supplier.getId().equals(product.getSupplierId()))
                .count();
        if (referencingProducts > 0) {
            throw new InventoryException("Cannot delete supplier '" + supplier.getName()
                    + "': " + referencingProducts + " product(s) still use it.");
        }
        SupplierDao.deleteById(supplier.getId());
    }

    public List<Supplier> listSuppliers() {
        return SupplierDao.findAll();
    }

    public Supplier requireSupplier(String id) throws EntityNotFoundException {
        if (id == null || id.isBlank()) {
            throw new EntityNotFoundException("Supplier not found.");
        }
        return SupplierDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    }

    /** Human-readable supplier name for a product row, never fails. */
    public String describeSupplier(String supplierId) {
        return SupplierDao.findById(supplierId)
                .map(Supplier::getName)
                .orElse("Unknown supplier");
    }
}
