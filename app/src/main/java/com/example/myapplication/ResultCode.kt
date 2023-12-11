package com.example.myapplication

import kotlin.Result

// работа с данными
interface UserDataSource {
    suspend fun getUsersByName(name: String): Result<List<User>>
    suspend fun getProfile(id: String): Result<Profile>
    suspend fun getUserFriends(id: String): Result<List<User>>
    suspend fun getCurrentUser(): Result<User>
    suspend fun addFriendForCurrent(userid: String, friendId: String): Result<Unit>
}

data class Profile(
    val id: String,
    val name: String,
    val friendsCount: Int,
    val isStudent: Boolean
)

data class User(
    val id: String,
    val name: String,
)

class MapperUserLogic {
    fun mapToUserLogic(profile: Profile): Result<UserLogic> =
        kotlin.runCatching {
            UserLogic.Full(profile.id, profile.name, profile.friendsCount)
        }

    fun mapToUserLogic(user: User): Result<UserLogic> =
        kotlin.runCatching {
            UserLogic.Short(user.id, user.name)
        }
}

inline fun <R, T> Result<R>.flatMap(transform: (R) -> Result<T>): Result<T> =
    fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )


class UserRepositoryImpl(
    private val userDataSource: UserDataSource,
    private val mapper: MapperUserLogic
) : UserRepository {
    override suspend fun getCurrentUserId(): Result<String> =
        userDataSource.getCurrentUser().map { it.id }

    override suspend fun getUser(id: String): Result<UserLogic> =
        userDataSource.getProfile(id).flatMap { mapper.mapToUserLogic(it) }

    override suspend fun findUsersByName(name: String): Result<List<UserLogic>> =
        userDataSource.getUsersByName(name).map { users ->
            users.mapNotNull {
                getUser(it.id).getOrNull()
            }
        }

    override suspend fun addFriend(userId: String, friendId: String) =
        userDataSource.addFriendForCurrent(userId, friendId)

    override suspend fun getUserFriends(id: String): Result<List<UserLogic>> =
        userDataSource.getUserFriends(id).map { users ->
            users.mapNotNull { user ->
                userDataSource.getProfile(user.id).fold(
                    onSuccess = { mapper.mapToUserLogic(it) },
                    onFailure = { mapper.mapToUserLogic(user)}
                ).getOrNull()
            }
        }
}


// слой бизнесс логики

sealed interface UserLogic {
    val id: String
    val name: String

    data class Full(
        override val id: String,
        override val name: String,
        val friendsCount: Int
    ) : UserLogic

    data class Short(
        override val id: String,
        override val name: String,
    ) : UserLogic
}

interface UserRepository {
    suspend fun getCurrentUserId(): Result<String>
    suspend fun getUser(id: String): Result<UserLogic>
    suspend fun findUsersByName(name: String): Result<List<UserLogic>>
    suspend fun addFriend(userId: String, friendId: String): Result<Unit>
    suspend fun getUserFriends(id: String): Result<List<UserLogic>>
}

class UserInteractor(
    private val repository: UserRepository
) {

    suspend fun getCurrentUser(): Result<UserLogic> =
        repository.getCurrentUserId().flatMap { repository.getUser(it) }

    suspend fun getUser(userId: String): Result<UserLogic> = repository.getUser(userId)

    suspend fun getMyFriends(): Result<List<UserLogic>> =
        repository.getCurrentUserId().flatMap { repository.getUserFriends(it) }


    suspend fun getUserFriends(id: String): Result<List<UserLogic>> =
        repository.getUserFriends(id)

    suspend fun addToMeAllFriendsWithName(name: String): Result<Int> =
        repository.getCurrentUserId().flatMap { currentUserId ->
            repository.findUsersByName(name).map { users ->
                users.mapNotNull { user ->
                    repository.addFriend(currentUserId, user.id).getOrNull()
                }.size
            }
        }
}

// место вызова интерактора
suspend fun sample(userInteractor: UserInteractor) {
    userInteractor.getCurrentUser().fold(
        onSuccess = {

        },
        onFailure = {

        }
    )
}
