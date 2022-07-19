@file:Suppress("FunctionName", "DEPRECATION")

package com.primex.core

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString

/**
 * Represents a Text Item.
 *
 * This interface can be used to send text [Resources] directly from the [ViewModel]s, [Services]; i.e,
 * directly from the non UI components
 */
@Immutable
@Stable
sealed interface Text

/**
 * A Raw text. i.e., [String]
 */
@JvmInline
@Immutable
private value class Raw(val value: AnnotatedString) : Text

/**
 * Constructs an [StringResource] String [Text] wrapper.
 */
@JvmInline
@Immutable
private value class StringResource(val id: Int) : Text

/**
 * A data class holding [Resource] String with [formatArgs]
 */
private data class StringResource2(val id: Int, val formatArgs: Array<out Any>) : Text {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringResource2

        if (id != other.id) return false
        if (!formatArgs.contentEquals(other.formatArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + formatArgs.contentHashCode()
        return result
    }
}

/**
 * A value class holding [Html] [Resource].
 * Currently only supports as are supported by [stringHtmlResource()] function.
 */
@JvmInline
@Immutable
private value class HtmlResource(@StringRes val id: Int) : Text

/**
 * @see TextPlural
 */
@JvmInline
@Immutable
private value class PluralResource(val packedValue: Long) : Text {

    @Stable
    val id: Int
        @PluralsRes
        get() = unpackInt1(packedValue)

    @Stable
    val quantity: Int
        get() = unpackInt1(packedValue)
}

/**
 * A data class holds [Plural] resource [String]s. with [formatArgs]
 */
private data class PluralResource2(
    val id: Int,
    val quantity: Int,
    val formatArgs: Array<out Any>
) : Text {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluralResource2

        if (id != other.id) return false
        if (quantity != other.quantity) return false
        if (!formatArgs.contentEquals(other.formatArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + quantity
        result = 31 * result + formatArgs.contentHashCode()
        return result
    }
}

/**
 * Composes the [Raw] Text resource wrapper.
 */
fun Text(value: String): Text = Raw(AnnotatedString(value))

/**
 * Composes the [Raw] Text resource wrapper.
 */
fun Text(value: AnnotatedString): Text = Raw(value)

/**
 * Composes the [StringResource] Text resource wrapper.
 */
fun Text(@StringRes id: Int): Text = StringResource(id)

/**
 * Composes the [StringResource] Text resource wrapper.
 */
fun Text(@StringRes id: Int, formatArgs: Array<out Any>): Text = StringResource2(id, formatArgs)

/**
 * Composes the [HtmlResource] Text resource wrapper.
 */
fun TextHtml(@StringRes id: Int): Text = HtmlResource(id)

/**
 * @see Plural
 */
fun TextPlural(@PluralsRes id: Int, quantity: Int): Text = PluralResource(
    packInts(id, quantity)
)

/**
 * Constructs an [PluralResource] from the given [Int]s
 * @param id    The desired resource identifier, as generated by the aapt tool. This integer encodes
 * the package, type, and resource entry. The value 0 is an invalid identifier.
 * @param quantity The number used to get the correct string for the current language's plural rules.
 * @param formatArgs Object: The format arguments that will be used for substitution.
 */
fun TextPlural(id: Int, quantity: Int, vararg formatArgs: Any): Text =
    PluralResource2(id, quantity, formatArgs)


/**
 * A simple function that returns the [Bundle] value inside this wrapper.
 *  It is either resource id from which this Wrapper was created or the raw text [String]
 */
val Text.raw: Any
    get() =
        when (this) {
            is HtmlResource -> id
            is PluralResource -> id
            is Raw -> value.text
            is StringResource -> id
            is StringResource2 -> id
            is PluralResource2 -> id
        }


private val String.asAnnotatedString
    inline get() =
        AnnotatedString(this)

/**
 * Unpacks the text wrapper to result [AnnotatedString]
 */
@Deprecated(
    "Use Text.collect", replaceWith = ReplaceWith(
        "spannedResource(value = this)", "com.primex.core.TextKt.resolveResource"
    )
)
val Text.obtain: AnnotatedString
    @Composable
    @ReadOnlyComposable
    @NonRestartableComposable
    get() =
        when (this) {
            is HtmlResource -> stringHtmlResource(id = this.id)
            is PluralResource ->
                stringQuantityResource(id = this.id, quantity = this.quantity)
                    .asAnnotatedString
            is PluralResource2 -> stringHtmlResource(this.id, this.quantity, this.formatArgs)
            is Raw -> this.value
            is StringResource -> stringResource(id = this.id).asAnnotatedString
            is StringResource2 -> stringResource(this.id, this.formatArgs).asAnnotatedString
        }


/**
 * Resolves the resource to [AnnotatedString]
 */
@Composable
@ReadOnlyComposable
@NonRestartableComposable
fun spannedResource(value: Text) = value.obtain

/**
 * **Note: Doesn't support collecting [HtmlResource] Strings.
 * @param text: The [Text] to collect.
 */
fun Resources.resolve(text: Text): AnnotatedString =
    when (text) {
        is HtmlResource -> error("Not supported when collecting from Resource")
        is PluralResource -> getQuantityString(text.id, text.quantity).asAnnotatedString
        is PluralResource2 -> getQuantityString(
            text.id,
            text.quantity,
            text.formatArgs
        ).asAnnotatedString
        is Raw -> text.value
        is StringResource -> getString(text.id).asAnnotatedString
        is StringResource2 -> getString(text.id, text.formatArgs).asAnnotatedString
    }

/**
 * @see resolve
 */
@JvmName("resolve2")
fun Resources.resolve(text: Text?): AnnotatedString? =
    if (text == null) null else resolve(text)



