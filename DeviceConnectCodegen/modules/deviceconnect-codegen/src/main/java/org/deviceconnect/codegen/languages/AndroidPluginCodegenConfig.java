package org.deviceconnect.codegen.languages;


import com.sun.javafx.sg.prism.NGShape;
import io.swagger.codegen.*;
import io.swagger.models.*;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class AndroidPluginCodegenConfig extends AbstractPluginCodegenConfig {

    private final String pluginModuleFolder = "plugin";
    private final String projectFolder = pluginModuleFolder + "/src/main";
    private final String sourceFolder = projectFolder + "/java";
    private final String resFolder = projectFolder + "/res";
    private final String profileSpecFolder = projectFolder + "/assets/api";
    private String invokerPackage;

    private final String apiDocPath = "docs/";
    private final String modelDocPath = "docs/";

    //----- AbstractPluginCodegenConfig ----//

    @Override
    protected String profileFileFolder() {
        String separator = File.separator;
        return outputFolder + separator + sourceFolder + separator + getProfilePackage().replace('.', File.separatorChar);
    }

    @Override
    protected String getProfileSpecFolder() {
        String separator = File.separator;
        return outputFolder + separator + profileSpecFolder;
    }

    @Override
    protected List<ProfileTemplate> prepareProfileTemplates(final String profileName, final Map<String, Object> properties) {
        final List<ProfileTemplate> profileTemplates = new ArrayList<>();
        String baseClassNamePrefix = getStandardClassName(profileName);
        final String baseClassName;
        final String profileClassName;
        final boolean isStandardProfile = baseClassNamePrefix != null;
        if (isStandardProfile) {
            baseClassName = baseClassNamePrefix + "Profile";
            profileClassName = getClassPrefix() + baseClassNamePrefix + "Profile";
        } else {
            baseClassName = "DConnectProfile";
            profileClassName = toUpperCapital(profileName) + "Profile";
        }
        properties.put("baseProfileClass", baseClassName);
        properties.put("profileClass", profileClassName);
        properties.put("profileNameDefinition", profileName);
        properties.put("profilePackage", getProfilePackage());
        properties.put("isStandardProfile", isStandardProfile);

        ((List<Object>) additionalProperties.get("supportedProfileNames")).add(new Object() { String name = profileName; });
        ((List<Object>) additionalProperties.get("supportedProfileClasses")).add(new Object() { String name = profileClassName;});

        ProfileTemplate template = new ProfileTemplate();
        template.templateFile = "profile.mustache";
        template.outputFile = profileClassName + ".java";
        profileTemplates.add(template);
        return profileTemplates;
    }

    private String getProfilePackage() {
        return invokerPackage + ".profiles";
    }

    //----- CodegenConfig ----//

    @Override
    public CodegenType getTag() { return CodegenType.OTHER; }

    @Override
    public String getName() { return "deviceConnectAndroidPlugin"; }

    @Override
    public String getHelp() { return "Generates a stub of Device Connect Plug-in for Android."; }

    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + "/" + apiDocPath).replace( '/', File.separatorChar );
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return ( outputFolder + "/" + modelDocPath ).replace( '/', File.separatorChar );
    }

    @Override
    public String toModelName(String name) {
        // add prefix, suffix if needed
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            name = modelNamePrefix + "_" + name;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            name = name + "_" + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        name = camelize(sanitizeName(name));

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            String modelName = "Model" + name;
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            String modelName = "Model" + name; // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();

            return getSwaggerType(p) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        invokerPackage = (String) additionalProperties.get("packageName");
        embeddedTemplateDir = templateDir = getName();
        additionalProperties.put("profilePackage", getProfilePackage());
        additionalProperties.put("pluginSdkVersion", "1.1.0");

        final String classPrefix = getClassPrefix();
        final String messageServiceClass = classPrefix + "MessageService";
        final String messageServiceProviderClass = classPrefix + "MessageServiceProvider";
        additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        additionalProperties.put("serviceName", classPrefix + " Service");
        additionalProperties.put("serviceId", classPrefix.toLowerCase() + "_service_id");
        additionalProperties.put("messageServiceClass", messageServiceClass);
        additionalProperties.put("messageServiceProviderClass", messageServiceProviderClass);

        // README
        supportingFiles.add(new SupportingFile("README.md.mustache", "", "README.md"));

        // ビルド設定ファイル
        supportingFiles.add(new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
        supportingFiles.add(new SupportingFile("manifest.mustache", projectFolder, "AndroidManifest.xml"));
        supportingFiles.add(new SupportingFile("root.build.gradle.mustache", "", "build.gradle"));
        supportingFiles.add(new SupportingFile("plugin.build.gradle.mustache", pluginModuleFolder, "build.gradle"));
        supportingFiles.add(new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
        supportingFiles.add(new SupportingFile("deviceplugin.xml.mustache", resFolder + "/xml", "deviceplugin.xml"));
        supportingFiles.add(new SupportingFile("string.xml.mustache", resFolder + "/values", "string.xml"));

        // アプリアイコン画像
        supportingFiles.add(new SupportingFile("res/drawable-mdpi/ic_launcher.png", resFolder + "/drawable-mdpi/", "ic_launcher.png"));
        supportingFiles.add(new SupportingFile("res/drawable-hdpi/ic_launcher.png", resFolder + "/drawable-hdpi/", "ic_launcher.png"));
        supportingFiles.add(new SupportingFile("res/drawable-xhdpi/ic_launcher.png", resFolder + "/drawable-xhdpi/", "ic_launcher.png"));
        supportingFiles.add(new SupportingFile("res/drawable-xxhdpi/ic_launcher.png", resFolder + "/drawable-xxhdpi/", "ic_launcher.png"));

        // 実装ファイル
        final String packageFolder = (sourceFolder + File.separator + invokerPackage).replace(".", File.separator);
        supportingFiles.add(new SupportingFile("MessageServiceProvider.java.mustache", packageFolder, messageServiceProviderClass + ".java"));
        supportingFiles.add(new SupportingFile("MessageService.java.mustache", packageFolder, messageServiceClass + ".java"));
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        return super.postProcessOperations(objs);
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toApiDocFilename( String name ) {
        return toApiName( name );
    }

    @Override
    public String toModelDocFilename( String name ) {
        return toModelName( name );
    }

    //----- DefaultCodegen ----//

    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type) || type.indexOf(".") >= 0 ||
                    type.equals("Map") || type.equals("List") ||
                    type.equals("File") || type.equals("Date")) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        return toModelName(type);
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = camelize(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        String example;

        if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "Short".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Long".equals(type)) {
            if (example == null) {
                example = "56";
            }
            example = example + "L";
        } else if ("Float".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            example = example + "F";
        } else if ("Double".equals(type)) {
            example = "3.4";
            example = example + "D";
        } else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        } else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "new File(\"" + escapeText(example) + "\")";
        } else if ("Date".equals(type)) {
            example = "new Date()";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + type + "()";
        }

        if (example == null) {
            example = "null";
        } else if (Boolean.TRUE.equals(p.isListContainer)) {
            example = "Arrays.asList(" + example + ")";
        } else if (Boolean.TRUE.equals(p.isMapContainer)) {
            example = "new HashMap()";
        }

        p.example = example;
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        operationId = camelize(sanitizeName(operationId), true);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, true);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return operationId;
    }

    @Override
    protected String getDeclaration(final Parameter p) {
        String type;
        String format;
        if (p instanceof QueryParameter) {
            type = ((QueryParameter) p).getType();
            format = ((QueryParameter) p).getFormat();
        } else if (p instanceof FormParameter) {
            type = ((FormParameter) p).getType();
            format = ((FormParameter) p).getFormat();
        } else {
            return null;
        }

        String varName = p.getName();
        String typeName;
        boolean hasParser;
        if ("number".equals(type)) {
            if ("double".equals(format)) {
                typeName = "Double";
            } else {
                typeName = "Float";
            }
            hasParser = true;
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                typeName = "Long";
            } else {
                typeName = "Integer";
            }
            hasParser = true;
        } else if ("boolean".equals(type)) {
            typeName = "Boolean";
            hasParser = true;
        } else if ("string".equals(type) || "array".equals(type)) {
            typeName = "String";
            hasParser = false;
        } else if ("file".equals(type)) {
            typeName = "byte[]";
            hasParser = false;
        } else {
            typeName = "Object";
            hasParser = false;
        }
        String leftOperand = typeName + " " + varName;
        String rightOperand;
        if (hasParser) {
            rightOperand = "parse" + typeName + "(request, \"" + varName + "\")";
        } else {
            rightOperand = "(" + typeName + ") request.getExtras().get(\"" + varName + "\")";
        }
        return leftOperand + " = " + rightOperand + ";";
    }

    @Override
    protected List<String> getResponseCreation(final Swagger swagger, final Response response) {
        List<String> lines = new ArrayList<>();
        Property schema = response.getSchema();

        ObjectProperty root;
        if (schema instanceof ObjectProperty) {
            root = (ObjectProperty) schema;
        } else if (schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            if (isIgnoredDefinition(ref.getName())) {
                return lines;
            }
            Model model = findDefinition(swagger, ref.getSimpleRef());
            Map<String, Property> properties;
            if (model instanceof ComposedModel) {
                properties = getProperties(swagger, (ComposedModel) model);
            } else if (model instanceof ModelImpl) {
                properties = model.getProperties();
            } else {
                lines.add("// WARNING: レスポンスの定義が不正です.");
                return lines;
            }
            if (properties == null) {
                lines.add("// WARNING: レスポンスの定義が見つかりませんでした.");
                return lines;
            }
            root =  new ObjectProperty();
            root.setProperties(properties);
        } else {
            lines.add("// WARNING: レスポンスの定義が不正です.");
            return lines;
        }

        writeExampleResponse(root, "root", lines);
        return lines;
    }

    @Override
    protected List<String> getEventCreation(final Swagger swagger, final Response event) {
        List<String> lines = new ArrayList<>();
        Property schema = event.getSchema();

        ObjectProperty root;
        if (schema instanceof ObjectProperty) {
            root = (ObjectProperty) schema;
        } else if (schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            if (isIgnoredDefinition(ref.getName())) {
                return lines;
            }
            Model model = findDefinition(swagger, ref.getSimpleRef());
            Map<String, Property> properties;
            if (model instanceof ComposedModel) {
                properties = getProperties(swagger, (ComposedModel) model);
            } else if (model instanceof ModelImpl) {
                properties = model.getProperties();
            } else {
                lines.add("// WARNING: イベントの定義が不正です.");
                return lines;
            }
            if (properties == null) {
                lines.add("// WARNING: イベントの定義が見つかりませんでした.");
                return lines;
            }
            root =  new ObjectProperty();
            root.setProperties(properties);
        } else {
            lines.add("// WARNING: イベントの定義が不正です.");
            return lines;
        }

        writeExampleEvent(root, "root", lines);
        return lines;
    }

    private boolean isIgnoredDefinition(final String refName) {
        return "CommonResponse".equals(refName) || "CommonEvent".equals(refName);
    }

    private Map<String, Property> getProperties(final Swagger swagger, final ComposedModel parent) {
        Map<String, Property> result = new HashMap<>();
        Stack<ComposedModel> stack = new Stack<>();
        stack.push(parent);
        do {
            ComposedModel model = stack.pop();
            List<Model> children = model.getAllOf();
            for (Model child : children) {
                if (child instanceof ModelImpl) {
                    if (child.getProperties() != null) {
                        result.putAll(child.getProperties());
                    }
                } else if (child instanceof ComposedModel) {
                    stack.push((ComposedModel) child);
                } else if (child instanceof RefModel) {
                    String refName = ((RefModel) child).getSimpleRef();
                    if (isIgnoredDefinition(refName)) {
                        continue;
                    }
                    Model m = findDefinition(swagger, refName);
                    if (m == null) {
                        continue;
                    }
                    if (m.getProperties() != null) {
                        result.putAll(m.getProperties());
                    }
                }
            }
        } while (!stack.empty());
        return result;
    }

    private Model findDefinition(final Swagger swagger, final String simpleRef) {
        Map<String, Model> definitions = swagger.getDefinitions();
        if (definitions == null) {
            return null;
        }
        return definitions.get(simpleRef);
    }

    private void writeExampleResponse(final ObjectProperty root, final String rootName,
                                      final List<String> lines) {
        lines.add("Bundle " + rootName + " = response.getExtras();");
        writeExampleMessage(root, rootName, lines);
        lines.add("response.putExtras(" + rootName + ");");
    }

    private void writeExampleEvent(final ObjectProperty root, final String rootName,
                                   final List<String> lines) {
        lines.add("Bundle " + rootName + " = message.getExtras();");
        writeExampleMessage(root, rootName, lines);
        lines.add("message.putExtras(" + rootName + ");");
    }

    private void writeExampleMessage(final ObjectProperty root, final String rootName,
                                     final List<String> lines) {
        Map<String, Property> props = root.getProperties();
        if (props == null) {
            return;
        }
        for (Map.Entry<String, Property> propEntry : props.entrySet()) {
            String propName = propEntry.getKey();
            Property prop = propEntry.getValue();

            String type = prop.getType();
            String format = prop.getFormat();
            if ("array".equals(type)) {
                ArrayProperty arrayProp;
                if (!(prop instanceof  ArrayProperty)) {
                    continue;
                }
                arrayProp = (ArrayProperty) prop;
                Property itemsProp = arrayProp.getItems();
                String setterName = getSetterName(itemsProp.getType(), itemsProp.getFormat());
                if (setterName == null) {
                    continue;
                }
                lines.add(rootName + "." + setterName +  "Array(\"" + propName + "\", );"); // TODO パラメータ値の設定
            } else if ("object".equals(type)) {
                ObjectProperty objectProp;
                if (!(prop instanceof ObjectProperty)) {
                    continue;
                }
                objectProp = (ObjectProperty) prop;
                lines.add("Bundle " + propName + " = new Bundle();");
                writeExampleMessage(objectProp, propName, lines);
                lines.add(rootName  + ".putBundle(\"" + propName + "\", " + propName + ");");
            } else {
                String setterName = getSetterName(type, format);
                if (setterName == null) {
                    continue;
                }
                lines.add(rootName + "." + setterName +  "(\""+ propName + "\", " + getExampleValue(type, format) + ");");
            }
        }
    }

    private String getSetterName(final String type, final String format) {
        if ("boolean".equals(type)) {
            return "putBoolean";
        } else if ("string".equals(type)) {
            return "putString";
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return "putLong";
            } else {
                return "putInt";
            }
        } else if ("number".equals(type)) {
            if ("double".equals(format)) {
                return "putDouble";
            } else {
                return "putFloat";
            }
        } else {
            // 現状のプラグインでは下記のタイプは非対応.
            //  - file
            return null;
        }
    }

    private String getExampleValue(final String type, final String format) {
        if ("boolean".equals(type)) {
            return "false";
        } else if ("string".equals(type)) {
            return "null";
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return "0L";
            } else {
                return "0";
            }
        } else if ("number".equals(type)) {
            if ("double".equals(format)) {
                return "0.0d";
            } else {
                return "0.0f";
            }
        } else {
            // 現状のプラグインでは下記のタイプは非対応.
            //  - file
            return null;
        }
    }
}