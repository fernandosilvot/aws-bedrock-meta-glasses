package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.graphics.Bitmap
import android.util.Log
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.ContentBlock
import aws.sdk.kotlin.services.bedrockruntime.model.ConversationRole
import aws.sdk.kotlin.services.bedrockruntime.model.ConverseRequest
import aws.sdk.kotlin.services.bedrockruntime.model.ImageBlock
import aws.sdk.kotlin.services.bedrockruntime.model.ImageFormat
import aws.sdk.kotlin.services.bedrockruntime.model.ImageSource
import aws.sdk.kotlin.services.bedrockruntime.model.Message
import aws.sdk.kotlin.services.bedrockruntime.model.SystemContentBlock
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import com.meta.wearable.dat.externalsampleapps.cameraaccess.BuildConfig
import java.io.ByteArrayOutputStream

class BedrockClient {
    companion object {
        private const val TAG = "BedrockClient"
        private const val MODEL_ID = "us.anthropic.claude-sonnet-4-5-20250929-v1:0"
    }

    private val client = BedrockRuntimeClient {
        region = BuildConfig.AWS_REGION
        credentialsProvider = StaticCredentialsProvider(
            BuildConfig.AWS_ACCESS_KEY,
            BuildConfig.AWS_SECRET_KEY,
        )
    }

    suspend fun askWithImage(prompt: String, frame: Bitmap, systemPrompt: String): String {
        val imageBytes = bitmapToBytes(frame)
        val request = ConverseRequest {
            modelId = MODEL_ID
            system = listOf(SystemContentBlock.Text(systemPrompt))
            messages = listOf(
                Message {
                    role = ConversationRole.User
                    content = listOf(
                        ContentBlock.Image(
                            ImageBlock {
                                format = ImageFormat.Jpeg
                                source = ImageSource.Bytes(imageBytes)
                            }
                        ),
                        ContentBlock.Text(prompt),
                    )
                }
            )
            inferenceConfig {
                maxTokens = 300
                temperature = 0.7F
            }
        }
        val response = client.converse(request)
        return response.output!!.asMessage().content.first().asText()
    }

    suspend fun askTextOnly(prompt: String, systemPrompt: String): String {
        val request = ConverseRequest {
            modelId = MODEL_ID
            system = listOf(SystemContentBlock.Text(systemPrompt))
            messages = listOf(
                Message {
                    role = ConversationRole.User
                    content = listOf(ContentBlock.Text(prompt))
                }
            )
            inferenceConfig {
                maxTokens = 300
                temperature = 0.7F
            }
        }
        val response = client.converse(request)
        return response.output!!.asMessage().content.first().asText()
    }

    fun close() {
        client.close()
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val scaled = if (bitmap.width > 512) {
            val ratio = 512f / bitmap.width
            Bitmap.createScaledBitmap(bitmap, 512, (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return stream.toByteArray()
    }
}

private class StaticCredentialsProvider(
    private val accessKey: String,
    private val secretKey: String,
) : CredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials =
        Credentials(accessKeyId = accessKey, secretAccessKey = secretKey)
}
