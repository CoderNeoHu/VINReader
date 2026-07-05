package com.vinreader.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * NHTSA VPIC API - 完全免费，无需注册，美国交通部提供的车辆信息查询服务
 * https://vpic.nhtsa.dot.gov/api/
 */
interface VinApiService {

    @GET("vehicles/DecodeVinValues/{vin}?format=json")
    suspend fun decodeVin(@Path("vin", encoded = true) vin: String): VinDecodeResponse

    companion object {
        private const val BASE_URL = "https://vpic.nhtsa.dot.gov/api/"

        fun create(): VinApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VinApiService::class.java)
        }
    }
}

/** NHTSA 响应包装 */
data class VinDecodeResponse(
    val Count: Int,
    val Message: String,
    val SearchCriteria: String?,
    val Results: List<VinResult>
)

/** 单条 VIN 解析结果 */
data class VinResult(
    val ABS: String?,
    val ActiveDriverAssist: String?,
    val AdaptiveCruiseControl: String?,
    val AdaptiveDrivingBeam: String?,
    val AdaptiveHeadlamps: String?,
    val AirBagLocCurtain: String?,
    val AirBagLocFront: String?,
    val AirBagLocKnee: String?,
    val AirBagLocSide: String?,
    val AirBagLocSeatCushion: String?,
    val AntiTheftSystem: String?,
    val AutomaticPedestrianAlertingSound: String?,
    val AxleConfiguration: String?,
    val Axles: String?,
    val BackupCamera: String?,
    val BasePrice: String?,
    val BatteryAuxillary: String?,
    val BatteryInfo: String?,
    val BatteryType: String?,
    val BlindSpotIntervention: String?,
    val BlindSpotMonitor: String?,
    val BodyCabType: String?,
    val BodyClass: String?,
    val BrakeSystemDesc: String?,
    val BrakeSystemType: String?,
    val BusFloorConfigType: String?,
    val BusLength: String?,
    val BusType: String?,
    val CAN_AutoRev: String?,
    val CIIS: String?,
    val CashForClunkers: String?,
    val ChargerLevel: String?,
    val ChargerPower: String?,
    val CoolingType: String?,
    val CrankCaseMaterial: String?,
    val CruiseControl: String?,
    val CustomMotorcycleType: String?,
    val Cyl: String?,
    val DaytimeRunningLight: String?,
    val DestinationMarket: String?,
    val DisplacementCC: String?,
    val DisplacementCI: String?,
    val DisplacementL: String?,
    val DoError: String?,
    val DoorCount: String?,
    val DriveType: String?,
    val DriverAssist: String?,
    val DynamicBrakeSupport: String?,
    val EDR: String?,
    val ESC: String?,
    val EVDriveUnit: String?,
    val ElectrificationLevel: String?,
    val EngineConfiguration: String?,
    val EngineCycles: String?,
    val EngineHP: String?,
    val EngineHP_to: String?,
    val EngineKW: String?,
    val EngineManufacturer: String?,
    val EngineModel: String?,
    val EntertainmentSystem: String?,
    val ErrorCode: String?,
    val ErrorText: String?,
    val ForwardCollisionWarning: String?,
    val FrontAirBagPassenger: String?,
    val FrontWheelDiameter: String?,
    val FrontWheelWidth: String?,
    val FuelInjectionType: String?,
    val FuelTypePrimary: String?,
    val FuelTypeSecondary: String?,
    val GCWR: String?,
    val GCWR_to: String?,
    val GVWR: String?,
    val GVWR_to: String?,
    val GarageSharing: String?,
    val GearCount: String?,
    val Headlamps: String?,
    val Height: String?,
    val HeightInches: String?,
    val Horsepower: String?,
    val Interlock: String?,
    val KeylessIgnition: String?,
    val LaneDepartureWarning: String?,
    val LaneKeepSystem: String?,
    val Length: String?,
    val LengthInches: String?,
    val LicensePlate: String?,
    val LowerBeamHeadlampLightSource: String?,
    val Make: String?,
    val MakeID: String?,
    val Manufacturer: String?,
    val ManufacturerId: String?,
    val Model: String?,
    val ModelID: String?,
    val ModelYear: String?,
    val MotorcycleChassisType: String?,
    val MotorcycleSuspensionType: String?,
    val NCSABodyType: String?,
    val NCSAMake: String?,
    val NCSAModel: String?,
    val NSCBodyType: String?,
    val Note: String?,
    val OtherBusInfo: String?,
    val OtherEngineInfo: String?,
    val OtherMotorcycleInfo: String?,
    val OtherRestraintSystemInfo: String?,
    val OtherTrailerInfo: String?,
    val ParkAssist: String?,
    val PedestrianAutoBrake: String?,
    val PedestrianAutoHeadlamp: String?,
    val PedestrianDetection: String?,
    val PlantCity: String?,
    val PlantCompanyName: String?,
    val PlantCountry: String?,
    val PlantState: String?,
    val PossibleValues: String?,
    val Pretensioner: String?,
    val RearCrossTrafficAlert: String?,
    val RearWheelDiameter: String?,
    val RearWheelWidth: String?,
    val RelativeHeight2: String?,
    val RelativeHeightV: String?,
    val SeatBeltsAll: String?,
    val SeatRows: String?,
    val Seats: String?,
    val SemiautomaticHeadlampBeamSwitching: String?,
    val Series: String?,
    val Series2: String?,
    val SteeringLocation: String?,
    val TPMS: String?,
    val TopSpeedData: String?,
    val TrackWidth: String?,
    val TractionControl: String?,
    val TrailerBodyType: String?,
    val TrailerLength: String?,
    val TrailerType: String?,
    val TransmissionSpeeds: String?,
    val TransmissionStyle: String?,
    val Trim: String?,
    val Turbo: String?,
    val VIN: String?,
    val ValveTrainDesign: String?,
    val VehicleType: String?,
    val WheelBaseLong: String?,
    val WheelBaseShort: String?,
    val WheelSizeFront: String?,
    val WheelSizeRear: String?,
    val Wheels: String?,
    val Width: String?,
    val WidthInches: String?,
    val Wpl: String?
) {
    /** 提取关键信息用于展示 */
    fun toDisplayMap(): Map<String, String> {
        val map = linkedMapOf(
            "VIN" to (VIN ?: "未知"),
            "品牌" to (Make ?: "未知"),
            "制造商" to (Manufacturer ?: "未知"),
            "型号" to (Model ?: "未知"),
            "车系" to (Series ?: "未知"),
            "年款" to (ModelYear ?: "未知"),
            "车型" to (VehicleType ?: "未知"),
            "车身类型" to (BodyClass ?: "未知"),
            "车门数" to (DoorCount ?: "未知"),
            "发动机" to (EngineModel ?: "未知"),
            "排量(L)" to (DisplacementL ?: "未知"),
            "马力(HP)" to (EngineHP ?: ""),
            "气缸数" to (Cyl ?: "未知"),
            "燃油类型" to (FuelTypePrimary ?: "未知"),
            "驱动方式" to (DriveType ?: "未知"),
            "变速箱" to (TransmissionStyle ?: "未知"),
            "变速档位数" to (TransmissionSpeeds ?: "未知"),
            "内饰级别" to (Trim ?: "未知"),
            "防抱死系统(ABS)" to (ABS ?: "未知"),
            "牵引力控制" to (TractionControl ?: "未知"),
            "电子稳定控制" to (ESC ?: "未知"),
            "轮胎压力监测" to (TPMS ?: "未知"),
            "制造工厂" to (PlantCompanyName ?: "未知"),
            "所在城市" to (PlantCity ?: "未知"),
            "所在州/省" to (PlantState ?: "未知"),
            "所在国家" to (PlantCountry ?: "未知"),
            "长度(mm)" to (Length ?: "未知"),
            "宽度(mm)" to (Width ?: "未知"),
            "高度(mm)" to (Height ?: "未知"),
            "轴距(长)" to (WheelBaseLong ?: "未知"),
            "轴距(短)" to (WheelBaseShort ?: "未知"),
            "整备质量" to (GCWR ?: "未知"),
            "基础价格" to (BasePrice ?: "未知"),
            "错误信息" to (ErrorText ?: "")
        )
        // 过滤掉空值和 "未知"
        return map.filter { (_, v) -> v.isNotBlank() && v != "未知" && v != "0" }
    }
}
