package traffictwin.dsl

data class ProgramNode(
    val statements: List<TopStatement>
)

sealed interface TopStatement

data class CityNode(
    val name: String,
    val items: List<CityItem>
) : TopStatement

data class LetNode(
    val name: String,
    val expression: ExpressionNode
) : TopStatement, CityItem, RoadStatement, AreaStatement, ParkingStatement

data object NilNode : TopStatement, CityItem, RoadStatement, AreaStatement, ParkingStatement

sealed interface CityItem

data class RoadNode(
    val name: String,
    val statements: List<RoadStatement>
) : CityItem

data class BuildingNode(
    val name: String,
    val statements: List<AreaStatement>
) : CityItem

data class ParkingNode(
    val name: String,
    val statements: List<ParkingStatement>
) : CityItem

data class ParkNode(
    val name: String,
    val statements: List<AreaStatement>
) : CityItem

data class ZoneNode(
    val name: String,
    val statements: List<AreaStatement>
) : CityItem

data class JunctionNode(
    val name: String?,
    val point: PointNode
) : CityItem

data class MarkerNode(
    val name: String?,
    val point: PointNode
) : CityItem

data class SensorNode(
    val name: String,
    val point: PointNode,
    val metadata: List<MetadataNode>
) : CityItem

data class QueryNode(
    val name: String,
    val statements: List<QueryStatement>
) : CityItem

data class MetadataNode(
    val key: String,
    val value: ExpressionNode
) : CityItem, RoadStatement, AreaStatement, ParkingStatement

sealed interface RoadStatement
sealed interface AreaStatement
sealed interface ParkingStatement
sealed interface QueryStatement

data class GeometryStatement(
    val geometry: GeometryNode
) : RoadStatement, AreaStatement

data class RoadTypeStatement(
    val type: String
) : RoadStatement

data class RoadRelationStatement(
    val relation: String
) : RoadStatement

data class RoadStateStatement(
    val state: RoadState
) : RoadStatement

data class SpeedLimitStatement(
    val expression: ExpressionNode
) : RoadStatement

data class LanesStatement(
    val expression: ExpressionNode
) : RoadStatement

data class OnewayStatement(
    val value: Boolean
) : RoadStatement

data class ParkingIdStatement(
    val id: Int
) : ParkingStatement

data class ParkingPointStatement(
    val point: PointNode
) : ParkingStatement

data class CapacityStatement(
    val expression: ExpressionNode
) : ParkingStatement

data class OccupiedStatement(
    val expression: ExpressionNode
) : ParkingStatement

data class PaymentStatement(
    val paymentType: PaymentType
) : ParkingStatement

data class ParkingStatusStatement(
    val status: ParkingStatus
) : ParkingStatement

sealed interface GeometryNode

data class LineGeometry(
    val from: PointNode,
    val to: PointNode
) : GeometryNode

data class BendGeometry(
    val from: PointNode,
    val to: PointNode,
    val amount: ExpressionNode
) : GeometryNode

data class PolylineGeometry(
    val points: List<PointNode>
) : GeometryNode

data class PolygonGeometry(
    val points: List<PointNode>
) : GeometryNode

data class BoxGeometry(
    val first: PointNode,
    val second: PointNode
) : GeometryNode

data class CircleGeometry(
    val center: PointNode,
    val radius: ExpressionNode
) : GeometryNode

data class PointNode(
    val x: ExpressionNode,
    val y: ExpressionNode
)

sealed interface ExpressionNode

data class NumberExpression(
    val value: Double
) : ExpressionNode

data class StringExpression(
    val value: String
) : ExpressionNode

data class BoolExpression(
    val value: Boolean
) : ExpressionNode

data class IdentifierExpression(
    val name: String
) : ExpressionNode

data class PointExpression(
    val point: PointNode
) : ExpressionNode

data class BinaryExpression(
    val left: ExpressionNode,
    val operator: String,
    val right: ExpressionNode
) : ExpressionNode

data class FunctionCallExpression(
    val functionName: String,
    val argument: ExpressionNode
) : ExpressionNode

data class NearbyQueryStatement(
    val point: ExpressionNode,
    val radius: ExpressionNode,
    val target: QueryTarget
) : QueryStatement

data class WhereQueryStatement(
    val condition: ConditionNode
) : QueryStatement

data class SortByQueryStatement(
    val identifier: String
) : QueryStatement

data class HighlightQueryStatement(
    val identifier: String
) : QueryStatement

data class ConditionNode(
    val left: ExpressionNode,
    val operator: String,
    val right: ExpressionNode
)