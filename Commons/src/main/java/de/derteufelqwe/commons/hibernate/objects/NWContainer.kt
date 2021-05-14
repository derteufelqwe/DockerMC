package de.derteufelqwe.commons.hibernate.objects

import de.derteufelqwe.commons.reflectionToString
import lombok.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.Type
import java.sql.Timestamp
import javax.persistence.*

/**
 *
 */
@Entity
@Table(
    name = "nw_containers",
    indexes = [
        Index(name = "nwcontainer_NAME_IDX", columnList = "name"),
    ]
)
data class NWContainer(
    @Id
    @Type(type = "text")
    var id: String? = null,

    @Type(type = "text")
    var name: String? = null,

    @Type(type = "text")
    var nodeID: String? = null,

    var startTime: Timestamp? = null,

    var stopTime: Timestamp? = null,

    @OneToMany(mappedBy = "nwContainer", fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OrderBy("timestamp asc")
    val logs: List<Log> = ArrayList(),

) {
    constructor() : this(null)

    constructor(id: String) : this(id = id, name = null)

    override fun toString(): String {
        return this.reflectionToString("logs")
    }
}
