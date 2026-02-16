# Lease Template Markdown Files

This directory contains default lease agreement templates that are automatically loaded into the database on first application startup.

## Overview

These markdown files are used by `DefaultLeaseTemplatesInitializer` to seed the database with example lease templates. Users can then customize these templates or create their own through the UI.

**⚠️ Important**: These templates are examples only and should not be used for actual legal purposes without review by a qualified attorney.

## Template Parameters

Templates use placeholder syntax: `{{parameter_name}}`. During lease creation, these placeholders are replaced with actual values.

### System Parameters (Auto-populated)

These parameters are automatically filled from lease and property data:

| Parameter | Description | Example Value |
|-----------|-------------|---------------|
| `{{property_name}}` | Legal name of the property | "Sunset Apartments LLC" |
| `{{unit_number}}` | Unit identifier | "Apt 12B" or "Suite 200" |
| `{{start_date}}` | Lease start date | "2024-01-01" |
| `{{end_date}}` | Lease end date | "2024-12-31" |
| `{{rent_amount}}` | Monthly rent amount | "1500.00" |
| `{{rent_due_day}}` | Day of month rent is due | "1" (for 1st of month) |
| `{{security_deposit}}` | Security deposit amount | "3000.00" or "N/A" |

**Note**: System parameters are defined in `LeaseTemplateRenderer.java` and cannot be customized per template.

### Custom Parameters (Template-specific)

Custom parameters are defined per template and can have default values. Users can override these when creating a lease.

**Residential Template Examples**:
- `{{parking_spaces}}` - Number of parking spaces included
- `{{pet_policy}}` - Pet allowance policy
- `{{utilities_included}}` - Which utilities are included in rent
- `{{maintenance_responsibility}}` - Maintenance division between parties

**Commercial Template Examples**:
- `{{permitted_use}}` - Allowed business activities
- `{{operating_hours}}` - Business operating hours
- `{{common_area_maintenance}}` - CAM fee responsibility
- `{{signage_rights}}` - Signage permissions and restrictions

## File Format

### File Naming
- Use lowercase with hyphens: `residential-lease-template.md`
- Use descriptive names: `commercial-lease-template.md`, `month-to-month-lease-template.md`

### Markdown Guidelines

1. **Use standard markdown syntax** - Headers, lists, tables, bold, italic, etc.
2. **Start with a clear title** - Use `# ` for the main heading
3. **Include legal disclaimers** - Always state this is an example template
4. **Organize with sections** - Use `## ` for major sections (Parties, Terms, Rent, etc.)
5. **Use placeholders** - Wrap parameters in double braces: `{{parameter_name}}`
6. **Be clear and readable** - Lease agreements should be understandable by non-lawyers

### Structure Recommendations

A well-structured lease template should include:

1. **Title** - Clear heading indicating the type of lease
2. **Disclaimer** - Legal notice that this is an example
3. **Parties** - Landlord and tenant identification
4. **Property Description** - Address and unit details
5. **Lease Term** - Start and end dates
6. **Rent Terms** - Amount, due date, payment method
7. **Use of Premises** - Permitted uses and restrictions
8. **Responsibilities** - Tenant and landlord obligations
9. **Fees and Penalties** - Late fees, damages, etc.
10. **Termination** - Notice requirements and conditions
11. **Signatures** - Signature blocks
12. **Closing Disclaimer** - Reiterate example/consultation notice

## Adding New Templates

To add a new lease template to the default set:

### 1. Create the Markdown File

Create a new `.md` file in this directory:

```bash
touch src/main/resources/lease-templates/my-new-template.md
```

### 2. Write the Template Content

Follow the format and guidelines above. Use system parameters and define any custom parameters you need.

### 3. Update the Initializer

Edit `DefaultLeaseTemplatesInitializer.java`:

```java
private void createMyNewTemplate() {
    String markdown = loadMarkdownFromResources("my-new-template.md");

    LeaseTemplate template = LeaseTemplate.builder()
        .name("My New Lease Agreement")
        .versionTag("v1.0")
        .templateMarkdown(markdown)
        .defaultLateFeeType(LateFeeType.FLAT_FEE)
        .defaultLateFeeAmount(new BigDecimal("50.00"))
        .defaultNoticePeriodDays(60)
        .templateParameters(Map.of(
            "custom_param_1", "default value 1",
            "custom_param_2", "default value 2"
        ))
        .active(true)
        .build();

    repository.save(template);
    log.info("[Data Init] Created: {}", template.getName());
}
```

### 4. Call from run() Method

Add the call in the `run()` method:

```java
@Override
@Transactional
public void run(ApplicationArguments args) {
    long count = repository.count();
    if (count > 0) {
        log.info("[Data Init] Found {} existing lease template(s), skipping defaults", count);
        return;
    }

    log.info("[Data Init] No lease templates found. Creating default templates...");

    createResidentialLeaseTemplate();
    createCommercialLeaseTemplate();
    createMyNewTemplate(); // Add your new template here

    log.info("[Data Init] Default lease templates created successfully");
}
```

### 5. Rebuild and Test

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Check the logs for `[Data Init]` messages to confirm templates were created.

## Modifying Existing Templates

You can edit the markdown files directly. Changes will take effect on the next fresh database initialization.

**Note**: These files only affect the *initial* database seed. Once templates are in the database, they are managed through the application. Editing these files won't update existing database records.

To update templates in an existing database:
1. Use the Lease Templates API to update via the UI or API calls
2. Or manually update the database records
3. Or drop the templates table and restart the app (⚠️ only for dev/test environments)

## Best Practices

### ✅ Do
- Use clear, plain language
- Include comprehensive legal disclaimers
- Organize content logically with sections
- Use consistent formatting
- Test placeholders render correctly
- Consider mobile/PDF rendering

### ❌ Don't
- Provide legal advice or claim templates are legally binding
- Use region-specific legal jargon without disclaimers
- Include PII or real party names in examples
- Use complex formatting that may not render well in all outputs
- Forget to use placeholders for dynamic content

## Related Files

- **Java Initializer**: `src/main/java/com/akandiah/propmanager/features/lease/service/DefaultLeaseTemplatesInitializer.java`
- **Domain Model**: `src/main/java/com/akandiah/propmanager/features/lease/domain/LeaseTemplate.java`
- **Renderer**: `src/main/java/com/akandiah/propmanager/features/lease/service/LeaseTemplateRenderer.java`

## Questions or Issues?

If you encounter issues with templates:
1. Check application logs for `[Data Init]` error messages
2. Verify file encoding is UTF-8
3. Ensure placeholders use correct syntax: `{{parameter_name}}`
4. Confirm the file is in the correct directory
5. Check for syntax errors in markdown

For template content questions, consult with a legal professional familiar with lease agreements in your jurisdiction.
