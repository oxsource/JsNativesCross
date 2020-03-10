package com.pizzk.android.compiler

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import pizzk.android.js.natives.annotate.JsModule
import pizzk.android.js.natives.annotate.JsProvide
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import java.util.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class AnnotateProcessor : AbstractProcessor() {
    private var debug: Boolean = true
    private var elements: Elements? = null
    private var filer: Filer? = null
    private var messager: Messager? = null

    override fun init(pe: ProcessingEnvironment?) {
        super.init(pe)
        elements = pe?.elementUtils
        filer = pe?.filer
        messager = pe?.messager
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return listOf(JsProvide::class, JsModule::class).map { it.java.canonicalName }.toHashSet()
    }

    override fun process(sets: MutableSet<out TypeElement>?, e: RoundEnvironment?): Boolean {
        val env: RoundEnvironment = e ?: return false
        val elements: Elements = elements ?: return false
        val filer: Filer = filer ?: return false
        //parse Provider
        try {
            val providers: Set<Element> = env.getElementsAnnotatedWith(JsProvide::class.java)
            log("providers size = ${providers.size}")
            if (providers.isEmpty()) return false
            val pElement: Element = providers.iterator().next()
            val provider: JsProvide = pElement.getAnnotation(JsProvide::class.java)
            log("provider need build = ${provider.build}")
            if (!provider.build) return false
            val pkgName: String = elements.getPackageOf(pElement).asType().toString()
            val clazzName: String = provider.name
            //parse Module
            val modules: Set<Element> = env.getElementsAnnotatedWith(JsModule::class.java)
            log("modules size = ${modules.size}")
            val properties: List<PropertySpec> = modules.map {
                val jsModule: JsModule = it.getAnnotation(JsModule::class.java)
                val value: String = jsModule.name.toUpperCase(Locale.getDefault())
                return@map PropertySpec.builder(value, String::class)
                    .initializer("%S", value)
                    .addModifiers(KModifier.PUBLIC, KModifier.CONST)
                    .build()
            }
            //ClassName statements
            val strType: ClassName = String::class.asClassName()
            val muteMapType = ClassName("kotlin.collections", "MutableMap")
            //
            val mapName = "maps"
            val mapParamType: ParameterizedTypeName = muteMapType.parameterizedBy(strType, strType)
            val mapPropertySpec: PropertySpec = PropertySpec.builder(mapName, mapParamType)
                .initializer("%T()", HashMap::class.asClassName())
                .addModifiers(KModifier.PRIVATE)
                .build()
            val mapCodeBlockBuilder: CodeBlock.Builder = CodeBlock.builder()
            modules.forEach {
                val jsModule: JsModule = it.getAnnotation(JsModule::class.java)
                val key: String = jsModule.name
                val clazz: String = it.asType().toString()
                mapCodeBlockBuilder.addStatement("$mapName[\"$key\"] = %S", clazz)
            }
            //create map function
            val mapsBuilder: FunSpec.Builder = FunSpec.builder("getMaps")
                .returns(Map::class.asClassName().parameterizedBy(strType, strType))
                .addStatement("return $mapName")
            val funSpecMaps: FunSpec = mapsBuilder.build()
            //create map function
            val provideBuilder: FunSpec.Builder = FunSpec.builder("provide")
                .addParameter("key", String::class)
                .returns(Any::class.asTypeName().copy(nullable = true))
                .addStatement("val name: String = getMaps()[key] ?: return null")
                .addStatement("return try { Class.forName(name).newInstance() } catch (e: Exception) { null }")
            val funSpecProvide: FunSpec = provideBuilder.build()
            //create class
            val typeSpecProvider: TypeSpec = TypeSpec.objectBuilder(clazzName)
                .addProperties(properties)
                .addProperty(mapPropertySpec)
                .addInitializerBlock(mapCodeBlockBuilder.build())
                .addFunction(funSpecMaps)
                .addFunction(funSpecProvide)
                .build()
            //save file
            val file: FileSpec = FileSpec.builder(pkgName, clazzName)
                .addType(typeSpecProvider)
                .build()
            file.writeTo(filer)
            log("process success")
        } catch (e: Exception) {
            log("process error: ${e.message}")
        }
        return true
    }

    private fun log(msg: String) {
        if (!debug) return
        val logger: Messager = messager ?: return
        logger.printMessage(Diagnostic.Kind.WARNING, msg)
    }
}

