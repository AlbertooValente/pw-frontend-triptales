package com.example.frontend_triptales

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//data class per le richieste
data class LoginRequest(
    val username: String,
    val password: String
)

data class CreateTripRequest(
    val name: String,
    val description: String
)

data class CreatePostRequest(
    val title: String,
    val description: String,
    val group: Int,
    val image: Int
)

data class CreateCommentRequest(
    val text: String
)

data class UserLikes(
    val user_id: Int,
    val username: String,
    val total_likes: Int
)

data class UserPosts(
    val user_id: Int,
    val username: String,
    val total_posts: Int
)

data class TokenResponse(val token: String)
data class MessageResponse(val message: String)
data class StatusResponse(val response: String)


interface TripTalesApi {
    //USERS
    @Multipart
    @POST("users/register/")
    suspend fun register(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Response<TokenResponse>

    @POST("users/login/")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @GET("users/profile/")
    suspend fun getUser(@Header("Authorization") token: String): Response<User>

    @Multipart
    @PATCH("users/profile/update/")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<User>

    @GET("users/my-trips/")
    suspend fun getTrips(@Header("Authorization") token: String): Response<List<Int>>

    @GET("users/profile/{id}/")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<User>


    //TRIPS
    @POST("trips/create/")
    suspend fun createTrip(
        @Header("Authorization") token: String,
        @Body trip: CreateTripRequest
    ): Response<Trip>

    @GET("trips/info/{id}/")
    suspend fun getTripInfo(
        @Header("Authorization") token: String,
        @Path("id") tripId: Int
    ): Response<Trip>

    @POST("trips/join/{id}/")
    suspend fun joinTrip(
        @Header("Authorization") token: String,
        @Path("id") tripId: Int
    ): Response<MessageResponse>

    @GET("trips/{id}/posts/")
    suspend fun getPosts(
        @Header("Authorization") token: String,
        @Path("id") tripId: Int
    ): Response<List<Post>>

    @GET("trips/{id}/top-like/")
    suspend fun getTopLike(
        @Header("Authorization") token: String,
        @Path("id") tripId: Int
    ): Response<List<Post>>

    @GET("trips/{id}/top-like-user/")
    suspend fun getTopLikeUser(
        @Header("Authorization") token: String,
        @Path("id") tripId: Int
    ): Response<List<UserLikes>>

    @GET("trips/{id}/top-posters/")
    suspend fun getTopPosters(
        @Header("Authorization") token: String,
        @Path("id") tripId: Int
    ): Response<List<UserPosts>>


    //IMAGES
    @Multipart
    @POST("images/create/")
    suspend fun caricaImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("description") description: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody
    ): Response<Image>

    @GET("images/{id}/")
    suspend fun getImage(
        @Header("Authorization") token: String,
        @Path("id") imageId: Int
    ): Response<Image>


    //POSTS
    @POST("posts/create/")
    suspend fun creaPost(
        @Header("Authorization") token: String,
        @Body request: CreatePostRequest
    ): Response<Post>

    @GET("posts/{id}/")
    suspend fun getPost(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<Post>
    /*
    Modifica post??
    */

    @DELETE("posts/{id}/")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<Unit>

    @POST("posts/{id}/like/")
    suspend fun giveLike(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<StatusResponse>

    @POST("posts/{id}/unlike/")
    suspend fun giveUnlike(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<StatusResponse>

    @GET("posts/{id}/comments/")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Path("id") postId: Int
    ): Response<List<Comment>>

    @POST("posts/{id}/comments/")
    suspend fun createComment(
        @Header("Authorization") token: String,
        @Path("id") postId: Int,
        @Body request: CreateCommentRequest
    ): Response<Comment>

    @DELETE("posts/{id}/comments/{idComm}/")
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Path("id") postId: Int,
        @Path("idComm") commentId: Int
    ): Response<Unit>
}

//salvo l'ip del server come variabile globale
object Constants {
    const val BASE_URL = "http://192.168.1.8:8000"  //da sostituire ogni volta con l'ip del backend
}

object RetrofitInstance {
    val api: TripTalesApi by lazy {
        Retrofit.Builder()
            .baseUrl("${Constants.BASE_URL}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TripTalesApi::class.java)
    }
}