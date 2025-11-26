package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Array Type Validator
 * Validates array/list values and their items
 */
public class ArrayValidator implements ValidationSchema {
    
    private final ValidationSchema itemsSchema;
    private final String description;
    private final Integer minItems;
    private final Integer maxItems;
    private final Boolean uniqueItems;
    
    public ArrayValidator(ValidationSchema itemsSchema, String description) {
        this(itemsSchema, description, null, null, null);
    }
    
    public ArrayValidator(ValidationSchema itemsSchema, String description,
                         Integer minItems, Integer maxItems, Boolean uniqueItems) {
        this.itemsSchema = itemsSchema;
        this.description = description;
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.uniqueItems = uniqueItems;
    }
    
    @Override
    public String getType() {
        return "array";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (value == null) {
            return ValidationResult.success();
        }
        
        if (!(value instanceof List)) {
            return ValidationResult.failure(
                "Expected array, but got: " + value.getClass().getSimpleName()
            );
        }
        
        List<?> list = (List<?>) value;
        
        // Validate array length
        if (minItems != null && list.size() < minItems) {
            return ValidationResult.failure(
                "Array must have at least " + minItems + " items, but got " + list.size()
            );
        }
        
        if (maxItems != null && list.size() > maxItems) {
            return ValidationResult.failure(
                "Array must have at most " + maxItems + " items, but got " + list.size()
            );
        }
        
        // Validate unique items
        if (Boolean.TRUE.equals(uniqueItems) && hasDuplicates(list)) {
            return ValidationResult.failure("Array must contain unique items");
        }
        
        // Validate each item
        if (itemsSchema != null) {
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                ValidationResult itemResult = itemsSchema.validate(item);
                
                if (!itemResult.isValid()) {
                    return ValidationResult.failure(
                        "Array item at index [" + i + "]: " + itemResult.getFirstError()
                    );
                }
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Check if list has duplicates
     */
    private boolean hasDuplicates(List<?> list) {
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Object item1 = list.get(i);
                Object item2 = list.get(j);
                
                if (item1 == null && item2 == null) {
                    return true;
                }
                
                if (item1 != null && item1.equals(item2)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public ValidationSchema getItemsSchema() {
        return itemsSchema;
    }
    
    @Override
    public String toString() {
        return "ArrayValidator{" +
               "itemsSchema=" + (itemsSchema != null ? itemsSchema.getType() : "null") +
               ", description='" + description + '\'' +
               '}';
    }
}