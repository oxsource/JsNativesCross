package com.pizzk.android.jsn.processor

import com.google.auto.service.AutoService
import com.pizzk.android.jsn.annotation.Module
import com.pizzk.android.jsn.annotation.Provider
import com.squareup.kotlinpoet.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.*
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class AnnotateProcessor : AbstractProcessor() {
    private var debug: Boolean = false
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
        return listOf(Provider::class, Module::class).map { it.java.canonicalName }.toHashSet()
    }

    override fun process(sets: MutableSet<out TypeElement>?, e: RoundEnvironment?): Boolean {
        val env: RoundEnvironment = e ?: return false
        val elements: Elements = elements ?: return false
        val filer: Filer = filer ?: return false
        //parse Provider
        try {
            val providers: Set<Element> = env.getElementsAnnotatedWith(Provider::class.java)
            log("providers size = ${providers.size}")
            if (providers.isEmpty()) return false
            val pElement: Element = providers.iterator().next()
            val provider: Provider = pElement.getAnnotation(Provider::class.java)
            log("provider is live = ${provider.live}")
            if (!provider.live) return false
            val pkgName: String = elements.getPackageOf(pElement).asType().toString()
            val clazzName: String = provider.name

            //parse Module
            val modules: Set<Element> = env.getElementsAnnotatedWith(Module::class.java)
            log("modules size = ${modules.size}")
            val clazzNames: List<String> = modules.map { it.asType().toString() }
            //ClassName statements
            val stringType = ClassName("kotlin", "String")
            val listType = ClassName("kotlin.collections", "List")
            val arrayListType = ClassName("kotlin.collections", "ArrayList")
            //create function
            val listName = "names"
            val modulesBuilder: FunSpec.Builder = FunSpec.builder("getModules")
                .returns(listType.parameterizedBy(stringType))
                .addStatement("val $listName = %T()", arrayListType.parameterizedBy(stringType))
            clazzNames.forEach { modulesBuilder.addStatement("$listName += %S", it) }
            modulesBuilder.addStatement("return $listName")
            val funSpecModules: FunSpec = modulesBuilder.build()
            //create class
            val typeSpecProvider: TypeSpec = TypeSpec.objectBuilder(clazzName)
                .addFunction(funSpecModules)
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
        logger.printMessage(Diagnostic.Kind.ERROR, msg)
    }
}
