package org.jboss.dependencytreeparser;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 *
 * @author panos
 */
public class TestsuiteParser {
    static HashSet<String> types = new HashSet<>();
    static HashMap<String,String> fields = new HashMap<>();
    static HashMap<String,String[]> methods = new HashMap<>();
    static HashSet<String> imports = new HashSet<>();
    static HashSet<String> typesNotResolved = new HashSet<>();
    static HashSet<String> methodsNotResolved = new HashSet<>();
    static ArrayList<ClassInfo> classInstanceCreations = new ArrayList<>();
    static ArrayList<MethodInfo> methodInvocations = new ArrayList<>();
    static String packageName;
    
    static HashMap<String,String> importedClassFields = new HashMap<>();

    public static HashMap<String, String[]> getMethods() {
        return methods;
    }

    public static void setMethods(HashMap<String, String[]> methods) {
        TestsuiteParser.methods = methods;
    }

    public static String getPackageName() {
        return packageName;
    }

    public static void setPackageName(String packageName) {
        TestsuiteParser.packageName = packageName;
    }

    public static HashMap<String, String> getImportedClassFields() {
        return importedClassFields;
    }

    public static void setImportedClassFields(HashMap<String, String> importedClassFields) {
        TestsuiteParser.importedClassFields = importedClassFields;
    }

    
    
    public static HashMap<String,String> getFields() {
        return fields;
    }

    public static void setFields(HashMap<String,String> fields) {
        TestsuiteParser.fields = fields;
    }

    public static HashSet<String> getTypes() {
        return types;
    }

    public static HashSet<String> getImports() {
        return imports;
    }

    public static void setImports(HashSet<String> imports) {
        TestsuiteParser.imports = imports;
    }

    public static void setTypes(HashSet<String> types) {
        TestsuiteParser.types = types;
    }

    public static ArrayList<ClassInfo> getClassInstanceCreations() {
        return classInstanceCreations;
    }

    public static void setClassInstanceCreations(ArrayList<ClassInfo> classInstanceCreations) {
        TestsuiteParser.classInstanceCreations = classInstanceCreations;
    }

    public static ArrayList<MethodInfo> getMethodInvocations() {
        return methodInvocations;
    }

    public static void setMethodInvocations(ArrayList<MethodInfo> methodInvocations) {
        TestsuiteParser.methodInvocations = methodInvocations;
    }
    
    public static void parse(String str) throws IOException {

        types.clear();
        fields.clear();
        methods.clear();
        classInstanceCreations.clear();
        typesNotResolved.clear();
        methodsNotResolved.clear();
        methodInvocations.clear();
        imports.clear();
   //     System.out.println("parse");
        ASTParser parser = ASTParser.newParser(AST.JLS3);
  //      System.out.println("parse");
        parser.setSource(readFileToString(str).toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

   //     System.out.println(readFileToString(str).toCharArray());
        
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        HashMap<String,String> declarations = new HashMap();

        cu.accept(new ASTVisitor() {

            Set names = new HashSet();
            int blockCount = 0;

            public boolean visit(FieldDeclaration node) {
                String name = node.fragments().get(0).toString().split("=")[0].trim();
                String type = node.getType().toString();
                
             //   System.out.println("Declaration of field '" + name + " of type " + type + "' at line"
             //                   + cu.getLineNumber(node.getStartPosition()) + " " + node.modifiers().toString()); 
                
                declarations.put(name, type);
                if(!node.modifiers().toString().contains("private")){
                //    System.out.println("modifiers : " + node.modifiers().contains("public") + " " + node.modifiers().contains("protected") + " " + node.modifiers().toString());
                    fields.put(name, type);
                }
                
                ArrayList<String> types0 = new ArrayList<>();
                
                String type2 =null;
                do {
                    if(type.contains("[")) {
                        type=type.replaceAll("\\[\\]", "");
                        if(type.contains("["))
                            type = type.substring(0,type.indexOf("["));
                    }
                    
                    if(type.contains("<")) {
                        String type3=type;
                        type = type.substring(0,type.indexOf("<"));
                        if (type3.substring(type3.indexOf("<")+1).startsWith("<>") || type3.substring(type.indexOf("<")+1).startsWith("<T>"))
                            type2=null;
                        else{
                            type2 = type3.substring(type3.indexOf("<")+1,type3.lastIndexOf(">"));
                            if(type2.contains(",")) {
                                if(type2.substring(0, type2.indexOf(",")).contains("<")){
                                    types0.add(type2);
                                }else{
                                    types0.add(type2.substring(0, type2.indexOf(",")));
                                    types0.add(type2.substring(type2.indexOf(",")+1));
                                }
                            }else {
                                types0.add(type2);
                            }
                        }
                        
                    }

                    types.addAll(Arrays.asList(type.split(" extends ")));
                    if(types0.size()!=0)
                        type = types0.remove(0);
                    else
                        type = null;
                }while(type!=null);
                
                return false;
            }

            public boolean visit(MethodDeclaration node) {
                if (node.getName().getIdentifier() != null) {
                    String returnType = null;
                    if(node.getReturnType2()==null)
                        returnType = "";
                    else
                        returnType = node.getReturnType2().toString();
                    
                //    System.out.println("Declaration of method '" + node.getName() + " return type : " + returnType + "' at line"
                //                + cu.getLineNumber(node.getStartPosition())); 
                    HashMap<String,String> bDeclarations = new HashMap();
                    bDeclarations.putAll(importedClassFields);
                    bDeclarations.putAll(declarations);
        
                    methods.put(node.getName().toString()+"_Return_Type", new String[]{returnType});
                    String[] methodParams = new String[node.parameters().size()];
                    List params = node.parameters();
                    int i=0;
                    for(Object s : params) {
                        String type = ((SingleVariableDeclaration)s).getType().toString();
                //        System.out.println("with param : " + type + " " + ((SingleVariableDeclaration)s).getName().toString());
                        
                        bDeclarations.put(((SingleVariableDeclaration)s).getName().toString(),type);
                        
                        ArrayList<String> types0 = new ArrayList<>();
                
                        String type2 = null;
                        String typeF = null;
                        do {
                            if(type.contains("[")) {
                                type=type.replaceAll("\\[\\]", "");
                                if(type.contains("["))
                                    type = type.substring(0,type.indexOf("["));
                            }else if(type.contains("<")) {
                                String type3=type;
                                type = type.substring(0,type.indexOf("<"));
                                if (type3.substring(type3.indexOf("<")+1).startsWith("<>") || type3.substring(type.indexOf("<")+1).startsWith("<T>"))
                                    type2=null;
                                else{
                                    type2 = type3.substring(type3.indexOf("<")+1,type3.lastIndexOf(">"));
                                    if(type2.contains(",")) {
                                        if(type2.substring(0, type2.indexOf(",")).contains("<")){
                                            types0.add(type2);
                                        }else{
                                            types0.add(type2.substring(0, type2.indexOf(",")));
                                            types0.add(type2.substring(type2.indexOf(",")+1));
                                        }
                                    }else {
                                        types0.add(type2);
                                    }
                                }

                            }

                            types.addAll(Arrays.asList(type.split(" extends ")));
                            if(types0.size()!=0){
                                type = types0.remove(0);
                                typeF = type;
                            }else
                                type = null;
                        }while(type!=null);
                        
                        methodParams[i++]=typeF;

                    }
                    
                    methods.put(node.getName().toString()+"_Return_Type", methodParams);

                    Block block = node.getBody();
                    
                //    if(block!=null)
                //        System.out.println("Block " + block.toString());
                    blockIterate(block, cu, bDeclarations);
                }
                return false;
            }
        
            public boolean visit(ImportDeclaration node) {
            //    System.out.println("Declaration of import '" + node.getName() + "' at line"
            //                    + cu.getLineNumber(node.getStartPosition())); 
                   
                imports.add(node.getName().toString());
                
                if(DependencyTreeMethods.jarClassPaths.containsKey(node.getName().toString())) {
                    importedClassFields = DependencyTreeMethods.listFieldsOfJarClass(DependencyTreeMethods.jarClassPaths.get(node.getName().toString()),node.getName().toString());
                    fields.putAll(importedClassFields);
                //    if(node.getName().toString().equals("org.jboss.as.controller.descriptions.ModelDescriptionConstants")){
                //        System.out.println("EEE : " + importedClassFields.keySet().toString());
                //    }
                }
                  
                
                return false;
            }
            
            public boolean visit(PackageDeclaration node) {
            //    System.out.println("Declaration of import '" + node.getName() + "' at line"
            //                    + cu.getLineNumber(node.getStartPosition())); 
                   
                packageName = node.getName().toString();
 
                return true;
            }
        });

    /*    System.out.println("Types : ");
        for(String type:types){
            System.out.println(type);
        }
        System.out.println("Imports : ");
        for(String import0:imports){
            System.out.println(import0);
        }
        System.out.println("TypesNotResolved : ");
        for(String typeNotResolved:typesNotResolved){
            System.out.println(typeNotResolved);
        }
        System.out.println("MethodsNotResolved : ");
        for(String methodNotResolved:methodsNotResolved){
            System.out.println(methodNotResolved);
        }
        System.out.println("ClassInstanceCreations : ");
        for(ClassInfo classInfo:classInstanceCreations){
            System.out.println(classInfo.className + " " + classInfo.params.toString() + " " + classInfo.isResolvedParam.toString());
        }
        System.out.println("MethodInvocations : ");
        for(MethodInfo methodInfo:methodInvocations){
            System.out.println(methodInfo.methodName + " " + methodInfo.expression + " " + methodInfo.params.toString() + " " + methodInfo.isResolvedParam.toString());
        }*/
    }

    public static HashSet<String> getTypesNotResolved() {
        return typesNotResolved;
    }

    public static void setTypesNotResolved(HashSet<String> typesNotResolved) {
        TestsuiteParser.typesNotResolved = typesNotResolved;
    }

    public static HashSet<String> getMethodsNotResolved() {
        return methodsNotResolved;
    }

    public static void setMethodsNotResolved(HashSet<String> methodsNotResolved) {
        TestsuiteParser.methodsNotResolved = methodsNotResolved;
    }

    private static void blockIterate(final Block block, final CompilationUnit cu, HashMap<String,String> blockDeclarations) {
        List<Statement> statements = new ArrayList<>();
        HashMap<String,String> bDeclarations = new HashMap();
        bDeclarations.putAll(blockDeclarations);
        
        if(block!=null)
            statements = block.statements();
    //    System.out.println("Statements : " + statements.toString());
        
        
        for (Statement s : statements) {
            

                s.accept(new ASTVisitor() { 

                    
                    
                    public boolean visit(SingleVariableDeclaration node) {
                        String name = node.getName().toString();
                        String type = node.getType().toString();
                    //    System.out.println("Declaration of variable '" + name + " " + type + "' at line"
                    //            + cu.getLineNumber(node.getStartPosition())); 
                        
                        bDeclarations.put(name,type);
                        
                        ArrayList<String> types0 = new ArrayList<>();
                
                        String type2 =null;
                        do {
                            if(type.contains("[")) {
                                type=type.replaceAll("\\[\\]", "");
                                if(type.contains("["))
                                    type = type.substring(0,type.indexOf("["));
                            }
                            
                            if(type.contains("<")) {
                                String type3=type;
                                type = type.substring(0,type.indexOf("<"));
                                if (type3.substring(type3.indexOf("<")+1).startsWith("<>") || type3.substring(type.indexOf("<")+1).startsWith("<T>"))
                                    type2=null;
                                else{
                                    type2 = type3.substring(type3.indexOf("<")+1,type3.lastIndexOf(">"));
                                    if(type2.contains(",")) {
                                        if(type2.substring(0, type2.indexOf(",")).contains("<")){
                                            types0.add(type2);
                                        }else{
                                            types0.add(type2.substring(0, type2.indexOf(",")));
                                            types0.add(type2.substring(type2.indexOf(",")+1));
                                        }
                                    }else {
                                        types0.add(type2);
                                    }
                                }

                            }

                            types.addAll(Arrays.asList(type.split(" extends ")));
                            if(types0.size()!=0)
                                type = types0.remove(0);
                            else
                                type = null;
                        }while(type!=null);
                        
                        return true; 
                    }
                    
                    public boolean visit(VariableDeclarationStatement node) {
                        String name = node.fragments().get(0).toString().split("=")[0].trim();
                        String type = node.getType().toString();
                   //     System.out.println("Declaration of variable '" + name + " " + type + "' at line"
                   //             + cu.getLineNumber(node.getStartPosition())); 
                        
                        bDeclarations.put(name,type);
                        
                        ArrayList<String> types0 = new ArrayList<>();
                
                        String type2 =null;
                        do {
                            if(type.contains("[")) {
                                type=type.replaceAll("\\[\\]", "");
                                if(type.contains("["))
                                    type = type.substring(0,type.indexOf("["));
                            }
                            
                            if(type.contains("<")) {
                                String type3=type;
                                type = type.substring(0,type.indexOf("<"));
                                if (type3.substring(type3.indexOf("<")+1).startsWith("<>") || type3.substring(type.indexOf("<")+1).startsWith("<T>"))
                                    type2=null;
                                else{
                                //    System.out.println("........" + type3);
                                    type2 = type3.substring(type3.indexOf("<")+1,type3.lastIndexOf(">"));
                                    if(type2.contains(",")) {
                                        if(type2.substring(0, type2.indexOf(",")).contains("<")){
                                            types0.add(type2);
                                        }else{
                                            types0.add(type2.substring(0, type2.indexOf(",")));
                                            types0.add(type2.substring(type2.indexOf(",")+1));
                                        }
                                    }else {
                                        types0.add(type2);
                                    }
                                }

                            }

                            types.addAll(Arrays.asList(type.split(" extends ")));
                            if(types0.size()!=0)
                                type = types0.remove(0);
                            else
                                type = null;
                        }while(type!=null);
                        
                        return true; 
                    }

                    public boolean visit(SimpleType node) {
                        
                        //    System.out.println("Usage of method/variable/field/parameter type : " + node.getName() + " at line "
                        //            + cu.getLineNumber(node.getStartPosition()) );
                            
                            String type = node.getName().toString();
                            String name = node.getName().toString();
                            
                            bDeclarations.put(name,type);
                            
                            ArrayList<String> types0 = new ArrayList<>();
                
                            String type2 =null;
                            do {
                                if(type.contains("[")) {
                                    type=type.replaceAll("\\[\\]", "");
                                    if(type.contains("["))
                                        type = type.substring(0,type.indexOf("["));
                                }
                                
                                if(type.contains("<")) {
                                    String type3=type;
                                    type = type.substring(0,type.indexOf("<"));
                                    if (type3.substring(type3.indexOf("<")+1).startsWith("<>") || type3.substring(type.indexOf("<")+1).startsWith("<T>"))
                                        type2=null;
                                    else{
                                        type2 = type3.substring(type3.indexOf("<")+1,type3.lastIndexOf(">"));
                                        if(type2.contains(",")) {
                                            if(type2.substring(0, type2.indexOf(",")).contains("<")){
                                                types0.add(type2);
                                            }else{
                                                types0.add(type2.substring(0, type2.indexOf(",")));
                                                types0.add(type2.substring(type2.indexOf(",")+1));
                                            }
                                        }else {
                                            types0.add(type2);
                                        }
                                    }

                                }

                                types.addAll(Arrays.asList(type.split(" extends ")));
                                if(types0.size()!=0)
                                    type = types0.remove(0);
                                else
                                    type = null;
                            }while(type!=null);

                            
                        
                        return true;
                    }
                    
                    public boolean visit(ClassInstanceCreation node) {
                        
                        //    System.out.println("ClassInstanceCreation " + node.getType() + " at line "
                        //            + cu.getLineNumber(node.getStartPosition()) );
                            
                            ClassInfo clInfo = new ClassInfo();
                            clInfo.className = node.getType().toString();
                            
                            
                            List params = node.arguments();
                            for(Object s : params) {
                                boolean resolved = true;
                                String arg = ((Expression)s).toString();

                                if(arg.contains("[") && !arg.contains("\""))
                                    arg = arg.substring(0,arg.indexOf("["));
                                if(arg.startsWith("\"") && arg.endsWith("\""))
                                    arg = "String";
                                else if(arg.startsWith("\'") && arg.endsWith("\'"))
                                    arg = "Character";
                                else if(arg.contains("+") && arg.contains("\""))
                                    arg = "String";
                                else if(arg.contains("instanceof"))
                                    arg = arg.substring(arg.indexOf("instanceof")+11);
                                else if(bDeclarations.containsKey(arg))
                                    arg = bDeclarations.get(arg);
                                else if(arg.equals("true") || arg.equals("false"))
                                    arg = "Boolean";
                                else if(arg.contains("==") || arg.contains(">") || arg.contains("<") || arg.contains("!=") || arg.contains(">=") || arg.contains("<="))
                                    arg = "Boolean";
                                else if(arg.contains("TimeUnit.SECONDS"))
                                    arg = "Numeric";
                                else if(arg.contains(".class"))
                                    arg = arg.replaceAll(".class", "");
                                else if(arg.startsWith("new ")){
                                    arg = arg.replaceAll("new ", "");
                                    if(arg.contains("("))
                                        arg = arg.substring(0, arg.indexOf("("));
                                    else if(arg.contains("["))
                                        arg = arg.substring(0, arg.indexOf("["));
                                }else if(NumberUtils.isNumber(arg)){
                                    arg = "Numeric";
                                }else if(arg.contains("-") || arg.contains("+") || arg.contains("*")){
                                    arg = "Numeric";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("String")) {
                                    arg = "String";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".is")) {
                                    arg = "Boolean";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".contains")) {
                                    arg = "Boolean";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Int")) {
                                    arg = "Integer";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Double")) {
                                    arg = "Double";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Boolean")) {
                                    arg = "Boolean";
                                }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".equals")) {
                                    arg = "Boolean";
                                }else {
                                    resolved = false;
                                    if(!arg.contains(".") && !arg.contains("(") && !arg.contains(")"))
                                        typesNotResolved.add(arg);
                                    else 
                                        methodsNotResolved.add(arg);
                                }
                                
                                clInfo.params.add(arg);
                                clInfo.isResolvedParam.add(resolved);
                        //        System.out.println("with args : " + arg);
                            }
                            
                            classInstanceCreations.add(clInfo);
                        
                        return true;
                    }
                    
                    public boolean visit(ConstructorInvocation node) {
                        
                        //    System.out.println("ConstructorInvocation " + node.toString() + " at line "
                        //            + cu.getLineNumber(node.getStartPosition()) );

                        List params = node.arguments();
                        for(Object s : params) {
                            boolean resolved = true;
                            String arg = ((Expression)s).toString();
                            if(arg.contains("[") && !arg.contains("\""))
                                arg = arg.substring(0,arg.indexOf("["));
                            if(arg.startsWith("\"") && arg.endsWith("\""))
                                arg = "String";
                            else if(arg.startsWith("\'") && arg.endsWith("\'"))
                                arg = "Character";
                            else if(arg.contains("+") && arg.contains("\""))
                                arg = "String";
                            else if(arg.contains("instanceof"))
                                arg = arg.substring(arg.indexOf("instanceof")+11);
                            else if(bDeclarations.containsKey(arg))
                                arg = bDeclarations.get(arg);
                            else if(arg.equals("true") || arg.equals("false"))
                                    arg = "Boolean";
                            else if(arg.contains("==") || arg.contains(">") || arg.contains("<") || arg.contains("!=") || arg.contains(">=") || arg.contains("<="))
                                    arg = "Boolean";
                            else if(arg.contains("TimeUnit.SECONDS"))
                                arg = "Numeric";
                            else if(arg.contains(".class"))
                                arg = arg.replaceAll(".class", "");
                            else if(arg.startsWith("new ")){
                                arg = arg.replaceAll("new ", "");
                                if(arg.contains("("))
                                    arg = arg.substring(0, arg.indexOf("("));
                                else if(arg.contains("["))
                                    arg = arg.substring(0, arg.indexOf("["));
                            }else if(NumberUtils.isNumber(arg)) {
                                arg = "Numeric";
                            }else if(arg.contains("-") || arg.contains("+") || arg.contains("*")){
                                arg = "Numeric";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("String")) {
                                arg = "String";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".is")) {
                                arg = "Boolean";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".contains")) {
                                arg = "Boolean";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Int")) {
                                arg = "Integer";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Double")) {
                                arg = "Double";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Boolean")) {
                                arg = "Boolean";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".equals")) {
                                arg = "Boolean";
                            }else {
                                resolved = false;
                                if(!arg.contains(".") && !arg.contains("(") && !arg.contains(")"))
                                    typesNotResolved.add(arg);
                                else 
                                    methodsNotResolved.add(arg);
                            }
                            
                        //    System.out.println("with args : " + arg);
                        }
                        
                        return true;
                    }

                /*    public boolean visit(NormalAnnotation node) {
                        
                        //    System.out.println("NormalAnnotation '" + node.getTypeName().getFullyQualifiedName() + "' at line "
                        //            + cu.getLineNumber(node.getStartPosition()) );

                         
                        
                        return true;
                    }*/
                    
                    public boolean visit(MethodInvocation node) {
                       
                        MethodInfo mInfo = new MethodInfo();
                        mInfo.expression =  node.getExpression()!=null?node.getExpression().toString():"";
                        mInfo.methodName = node.getName().toString();
                        
                        boolean methodUnResolved = false;
                        
                        if(node.getExpression()!=null) {
                            mInfo.expression = node.getExpression().toString().replaceAll("this.", "");
                            if(mInfo.expression.startsWith("\"") && mInfo.expression.endsWith("\""))
                                mInfo.expression = "String";
                            else if(mInfo.expression.equals("this"))
                                mInfo.expression = "";
                            else if(mInfo.expression.startsWith("new ")) {
                                mInfo.expression = mInfo.expression.replaceAll("new ", "");
                                if (mInfo.expression.contains("("))
                                    mInfo.expression=mInfo.expression.substring(0, mInfo.expression.indexOf("("));
                            }
                            if(mInfo.expression.contains("[")){
                                mInfo.expression = mInfo.expression.substring(0,mInfo.expression.indexOf("["));
                            }
                            if(!bDeclarations.containsKey(mInfo.expression)) {
                                methodsNotResolved.add(node.getExpression().toString()+"."+node.getName()+"("+node.arguments()+")");
                                methodUnResolved = true;
                            }else if(bDeclarations.get(mInfo.expression)!=null) {
                                if(!bDeclarations.get(mInfo.expression).contains("<"))
                                    mInfo.expression = bDeclarations.get(mInfo.expression);
                                else
                                    mInfo.expression = bDeclarations.get(mInfo.expression).substring(0, bDeclarations.get(mInfo.expression).indexOf("<"));
                            }
                            
                            if(mInfo.expression.contains(".") && Character.isLowerCase(mInfo.expression.substring(0, mInfo.expression.indexOf(".")).charAt(0)) && bDeclarations.containsKey(mInfo.expression.substring(0, mInfo.expression.indexOf(".")))){
                                mInfo.expression = bDeclarations.get(mInfo.expression.substring(0, mInfo.expression.indexOf("."))) + mInfo.expression.substring(mInfo.expression.indexOf("."));
                            }
                        }
                        
                    //     System.out.println("MethodInvocation: " + node.getName() + " at line "
                    //            + cu.getLineNumber(node.getStartPosition()) + " with arguments " + node.arguments() + " exp " + node.getExpression());
                        
                        List params = node.arguments();
                        for(Object s : params) {
                            boolean resolved = true;
                            String arg = ((Expression)s).toString();
                            if(arg.contains("[") && !arg.contains("\""))
                                arg = arg.substring(0,arg.indexOf("["));
                            if(arg.startsWith("\"") && arg.endsWith("\""))
                                arg = "String";
                            else if(arg.startsWith("\'") && arg.endsWith("\'"))
                                arg = "Character";
                            else if(arg.contains("+") && arg.contains("\""))
                                arg = "String";
                            else if(arg.contains("instanceof"))
                                arg = arg.substring(arg.indexOf("instanceof")+11);
                            else if(bDeclarations.containsKey(arg))
                                arg = bDeclarations.get(arg);
                            else if(arg.equals("true") || arg.equals("false"))
                                arg = "Boolean";
                            else if(arg.contains("==") || arg.contains(">") || arg.contains("<") || arg.contains("!=") || arg.contains(">=") || arg.contains("<="))
                                arg = "Boolean";
                            else if(arg.contains("TimeUnit.SECONDS"))
                                arg = "Numeric";
                            else if(arg.contains(".class"))
                                arg = arg.replaceAll(".class", "");
                            else if(arg.startsWith("new ")){
                                arg = arg.replaceAll("new ", "");
                                if(arg.contains("("))
                                    arg = arg.substring(0, arg.indexOf("("));
                                else if(arg.contains("["))
                                    arg = arg.substring(0, arg.indexOf("["));
                            }else if(NumberUtils.isNumber(arg)){
                                arg = "Numeric"; 
                            }else if(arg.contains("-") || arg.contains("+") || arg.contains("*")){
                                arg = "Numeric";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("String")) {
                                arg = "String";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".is")) {
                                arg = "Boolean";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".contains")) {
                                arg = "Boolean";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Int")) {
                                arg = "Integer";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Double")) {
                                arg = "Double";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).contains("Boolean")) {
                                arg = "Boolean";
                            }else if(arg.contains(".") && arg.substring(arg.lastIndexOf(".")).startsWith(".equals")) {
                                arg = "Boolean";
                            }else {
                                resolved = false;
                                if(!arg.contains(".") && !arg.contains("(") && !arg.contains(")"))
                                    typesNotResolved.add(arg);
                                else  if(!methodUnResolved) {
                                    methodsNotResolved.add(arg);
                                    methodUnResolved=true;
                                }
                            }
                            
                            mInfo.params.add(arg);
                            mInfo.isResolvedParam.add(resolved);
                            
                        //    System.out.println("with param : " + arg);
                        }
                        
                        methodInvocations.add(mInfo);

                        return true;
                    }

                    public boolean visit(Block node) {
                        if (node != null) {
                            
                            blockIterate(node, cu, bDeclarations);


                        }
                        return false;
                    }
                });
            
        }
    }
    
    //read file content into a string
    private static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }
    
    public static ArrayList<String> readAcceptedTypesFromFile(String filePath) throws IOException {
        ArrayList<String> acceptedTypes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        for(String line = reader.readLine(); line != null; line = reader.readLine()) {
            acceptedTypes.add(line);
        }

        reader.close();

        return acceptedTypes;
    }
    
    public static ArrayList<String> loadFieldsFromFile(String filePath) throws IOException {
        ArrayList<String> addedFields = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        for(String line = reader.readLine(); line != null; line = reader.readLine()) {
            addedFields.add(line);
        }

        reader.close();

        return addedFields;
    }
}

class ClassInfo{
    public String className = "";
    public ArrayList<String> params = new ArrayList<>();
    public ArrayList<Boolean> isResolvedParam = new ArrayList<>();
}

class MethodInfo{
    public String methodName = "";
    public String expression = "";
    public ArrayList<String> params = new ArrayList<>();
    public ArrayList<Boolean> isResolvedParam = new ArrayList<>();
}