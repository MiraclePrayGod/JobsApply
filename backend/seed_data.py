"""
Script para poblar la base de datos con datos de prueba (seeders)
Ejecutar: python seed_data.py
"""
import sys
import os
# Configurar encoding UTF-8 para Windows
if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    sys.stdout.reconfigure(encoding='utf-8') if hasattr(sys.stdout, 'reconfigure') else None

from app.database import SessionLocal, engine, Base
from app.models import (
    User, UserRole,
    Worker,
    Job, JobStatus, PaymentMethod,
    Commission, CommissionStatus,
    Rating,
    Message,
    WorkerSubscription, SubscriptionPlan, SubscriptionStatus,
    JobApplication
)
from app.utils.security import get_password_hash
from decimal import Decimal
from datetime import datetime, timedelta
import json

# Crear tablas si no existen
Base.metadata.create_all(bind=engine)

def seed_data():
    """Pobla la base de datos con datos de prueba"""
    db = SessionLocal()
    
    try:
        print("[INFO] Iniciando seeders...")
        
        # Limpiar datos existentes (opcional - comentar si quieres mantener datos)
        print("[INFO] Limpiando datos existentes...")
        db.query(Message).delete()
        db.query(Rating).delete()
        db.query(Commission).delete()
        db.query(JobApplication).delete()  # Limpiar aplicaciones antes de jobs
        db.query(Job).delete()
        db.query(WorkerSubscription).delete()  # Limpiar suscripciones antes de workers
        db.query(Worker).delete()
        db.query(User).delete()
        db.commit()
        
        # ============================================
        # 1. CREAR USUARIOS
        # ============================================
        print("[INFO] Creando usuarios...")
        
        # Clientes
        client1 = User(
            email="cliente1@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.CLIENT,
            full_name="María González",
            phone="987654321",
            profile_image_url="https://i.pravatar.cc/150?img=47"  # Foto de perfil
        )
        client2 = User(
            email="cliente2@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.CLIENT,
            full_name="Carlos Ramírez",
            phone="987654322",
            profile_image_url="https://i.pravatar.cc/150?img=15"  # Foto de perfil
        )
        client3 = User(
            email="cliente3@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.CLIENT,
            full_name="Ana Martínez",
            phone="987654323",
            profile_image_url="https://i.pravatar.cc/150?img=20"  # Foto de perfil
        )
        
        # Trabajadores
        worker_user1 = User(
            email="trabajador1@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.WORKER,
            full_name="Juan Pérez",
            phone="987654324",
            profile_image_url="https://i.pravatar.cc/150?img=12"  # Foto de perfil
        )
        worker_user2 = User(
            email="trabajador2@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.WORKER,
            full_name="Luis Sánchez",
            phone="987654325",
            profile_image_url="https://i.pravatar.cc/150?img=33"  # Foto de perfil
        )
        worker_user3 = User(
            email="trabajador3@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.WORKER,
            full_name="Pedro López",
            phone="987654326",
            profile_image_url="https://i.pravatar.cc/150?img=25"  # Foto de perfil
        )
        
        # Manager
        manager = User(
            email="manager@test.com",
            password_hash=get_password_hash("password123"),
            role=UserRole.MANAGER,
            full_name="Admin Manager",
            phone="987654327",
            profile_image_url="https://i.pravatar.cc/150?img=51"  # Foto de perfil
        )
        
        db.add_all([client1, client2, client3, worker_user1, worker_user2, worker_user3, manager])
        db.commit()
        db.refresh(client1)
        db.refresh(client2)
        db.refresh(client3)
        db.refresh(worker_user1)
        db.refresh(worker_user2)
        db.refresh(worker_user3)
        db.refresh(manager)
        
        print(f"[OK] Usuarios creados: {db.query(User).count()}")
        
        # ============================================
        # 2. CREAR PERFILES DE TRABAJADORES
        # ============================================
        print("[INFO] Creando perfiles de trabajadores...")
        
        worker1 = Worker(
            user_id=worker_user1.id,
            full_name="Juan Pérez",
            phone="987654324",
            services=json.dumps(["Plomería", "Electricidad"]),
            description="Experto en reparaciones de plomería y electricidad. Más de 10 años de experiencia. Trabajo rápido y eficiente, garantía en todos mis servicios.",
            district="San Isidro",
            is_available=True,
            yape_number="987654324",
            profile_image_url="https://i.pravatar.cc/150?img=12",  # Foto de perfil completa
            is_verified=True,  # Ya aplicó a trabajos, perfil verificado
            verification_photo_url="https://example.com/verification/juan-perez-dni.jpg",  # Foto de DNI verificada
            is_plus_active=True,  # Tiene suscripción Plus activa
            plus_expires_at=datetime.now() + timedelta(days=7)  # Expira en 7 días
        )
        
        worker2 = Worker(
            user_id=worker_user2.id,
            full_name="Luis Sánchez",
            phone="987654325",
            services=json.dumps(["Limpieza", "Pintura"]),
            description="Servicios de limpieza profesional y pintura de interiores y exteriores. Materiales de primera calidad, puntual y responsable.",
            district="Miraflores",
            is_available=True,
            yape_number="987654325",
            profile_image_url="https://i.pravatar.cc/150?img=33",  # Foto de perfil completa
            is_verified=True,  # Ya aplicó a trabajos, perfil verificado
            verification_photo_url="https://example.com/verification/luis-sanchez-dni.jpg",  # Foto de DNI verificada
            is_plus_active=False,  # No tiene suscripción Plus
            plus_expires_at=None
        )
        
        worker3 = Worker(
            user_id=worker_user3.id,
            full_name="Pedro López",
            phone="987654326",
            services=json.dumps(["Carpintería", "Plomería"]),
            description="Carpintero especializado en muebles y reparaciones de plomería.",
            district="La Molina",
            is_available=False,  # No disponible
            yape_number="987654326",
            is_verified=False,  # No verificado por defecto - debe subir foto de DNI
            verification_photo_url=None,
            is_plus_active=False,  # No tiene suscripción Plus
            plus_expires_at=None
        )
        
        db.add_all([worker1, worker2, worker3])
        db.commit()
        db.refresh(worker1)
        db.refresh(worker2)
        db.refresh(worker3)
        
        print(f"[OK] Trabajadores creados: {db.query(Worker).count()}")
        
        # ============================================
        # 2.5. CREAR SUSCRIPCIONES PLUS
        # ============================================
        print("[INFO] Creando suscripciones Plus...")
        
        # Suscripción semanal activa para worker1 (Juan Pérez)
        subscription1 = WorkerSubscription(
            worker_id=worker1.id,
            plan=SubscriptionPlan.WEEKLY,
            days=7,
            amount=Decimal("12.00"),
            status=SubscriptionStatus.ACTIVE,
            payment_method="yape",
            payment_code="987654324",
            valid_from=datetime.now() - timedelta(days=1),  # Empezó hace 1 día
            valid_until=datetime.now() + timedelta(days=6)   # Expira en 6 días (7 días total)
        )
        
        # Suscripción diaria expirada para worker2 (Luis Sánchez) - ejemplo de suscripción pasada
        subscription2 = WorkerSubscription(
            worker_id=worker2.id,
            plan=SubscriptionPlan.DAILY,
            days=1,
            amount=Decimal("2.00"),
            status=SubscriptionStatus.EXPIRED,
            payment_method="yape",
            payment_code="987654325",
            valid_from=datetime.now() - timedelta(days=5),
            valid_until=datetime.now() - timedelta(days=4)  # Expiró hace 4 días
        )
        
        db.add_all([subscription1, subscription2])
        db.commit()
        
        print(f"[OK] Suscripciones creadas: {db.query(WorkerSubscription).count()}")
        
        # ============================================
        # 3. CREAR TRABAJOS (JOBS)
        # ============================================
        print("[INFO] Creando trabajos...")
        
        # Trabajos PENDING (sin trabajador asignado) - 6 trabajos pendientes
        job1 = Job(
            client_id=client1.id,
            worker_id=None,
            title="Reparación de grifo en cocina",
            description="El grifo de la cocina tiene una fuga constante. Necesito reparación urgente.",
            service_type="Plomería",
            status=JobStatus.PENDING,
            payment_method=PaymentMethod.YAPE,
            base_fee=Decimal("80.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("80.00"),
            address="Av. Javier Prado 1234, San Isidro",
            latitude=Decimal("-12.0962"),
            longitude=Decimal("-77.0335")
        )
        
        job_pending2 = Job(
            client_id=client2.id,
            worker_id=None,
            title="Instalación de interruptores eléctricos",
            description="Necesito instalar 8 interruptores nuevos en mi casa. Requiero electricista con experiencia.",
            service_type="Electricidad",
            status=JobStatus.PENDING,
            payment_method=PaymentMethod.CASH,
            base_fee=Decimal("120.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("120.00"),
            address="Jr. Las Begonias 567, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        job_pending3 = Job(
            client_id=client3.id,
            worker_id=None,
            title="Limpieza de casa completa",
            description="Limpieza profunda de casa de 3 pisos. Incluye ventanas, baños y cocina.",
            service_type="Limpieza",
            status=JobStatus.PENDING,
            payment_method=PaymentMethod.YAPE,
            base_fee=Decimal("180.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("180.00"),
            address="Av. Larco 123, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        job_pending4 = Job(
            client_id=client1.id,
            worker_id=None,
            title="Pintura de fachada exterior",
            description="Pintar fachada de casa de 2 pisos. Color blanco. Requiero pintor profesional.",
            service_type="Pintura",
            status=JobStatus.PENDING,
            payment_method=PaymentMethod.CASH,
            base_fee=Decimal("400.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("400.00"),
            address="Av. Arequipa 890, San Isidro",
            latitude=Decimal("-12.0962"),
            longitude=Decimal("-77.0335")
        )
        
        job_pending5 = Job(
            client_id=client2.id,
            worker_id=None,
            title="Reparación de ducha",
            description="La ducha no calienta el agua. Necesito plomero urgente para revisar el calentador.",
            service_type="Plomería",
            status=JobStatus.PENDING,
            payment_method=PaymentMethod.YAPE,
            base_fee=Decimal("100.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("100.00"),
            address="Jr. Las Begonias 567, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        job_pending6 = Job(
            client_id=client3.id,
            worker_id=None,
            title="Carpintería: Reparación de puertas",
            description="Tengo 3 puertas que no cierran bien. Necesito carpintero para ajustar bisagras y cerraduras.",
            service_type="Carpintería",
            status=JobStatus.PENDING,
            payment_method=PaymentMethod.CASH,
            base_fee=Decimal("150.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("150.00"),
            address="Av. Larco 123, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        # Trabajo ACCEPTED (trabajador asignado, esperando inicio)
        job2 = Job(
            client_id=client2.id,
            worker_id=worker1.id,
            title="Instalación de lámparas LED",
            description="Necesito instalar 5 lámparas LED en el techo del salón.",
            service_type="Electricidad",
            status=JobStatus.ACCEPTED,
            payment_method=PaymentMethod.CASH,
            base_fee=Decimal("150.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("150.00"),
            address="Jr. Las Begonias 567, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        # Trabajo IN_ROUTE (trabajador en camino)
        job3 = Job(
            client_id=client1.id,
            worker_id=worker1.id,
            title="Reparación de tubería rota",
            description="Tubería principal rota en el baño, hay fuga de agua.",
            service_type="Plomería",
            status=JobStatus.IN_ROUTE,
            payment_method=PaymentMethod.YAPE,
            base_fee=Decimal("120.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("120.00"),
            address="Av. Arequipa 890, San Isidro",
            latitude=Decimal("-12.0962"),
            longitude=Decimal("-77.0335")
        )
        
        # Trabajo ON_SITE (trabajador llegó al lugar)
        job4 = Job(
            client_id=client3.id,
            worker_id=worker2.id,
            title="Limpieza profunda de oficina",
            description="Limpieza completa de oficina de 50m², incluye ventanas y alfombras.",
            service_type="Limpieza",
            status=JobStatus.ON_SITE,
            payment_method=PaymentMethod.CASH,
            base_fee=Decimal("200.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("200.00"),
            address="Av. Larco 123, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        # Trabajo IN_PROGRESS (en progreso)
        job5 = Job(
            client_id=client2.id,
            worker_id=worker2.id,
            title="Pintura de sala y comedor",
            description="Pintar sala y comedor, 2 capas, color beige.",
            service_type="Pintura",
            status=JobStatus.IN_PROGRESS,
            payment_method=PaymentMethod.YAPE,
            base_fee=Decimal("300.00"),
            extras=Decimal("50.00"),  # Extra por materiales adicionales
            total_amount=Decimal("350.00"),
            address="Jr. Las Begonias 567, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304"),
            started_at=datetime.now() - timedelta(hours=2)
        )
        
        # Trabajo COMPLETED (completado)
        job6 = Job(
            client_id=client1.id,
            worker_id=worker1.id,
            title="Instalación de sistema eléctrico",
            description="Instalación completa de sistema eléctrico en nueva construcción.",
            service_type="Electricidad",
            status=JobStatus.COMPLETED,
            payment_method=PaymentMethod.YAPE,
            base_fee=Decimal("500.00"),
            extras=Decimal("100.00"),
            total_amount=Decimal("600.00"),
            address="Av. Javier Prado 1234, San Isidro",
            latitude=Decimal("-12.0962"),
            longitude=Decimal("-77.0335"),
            started_at=datetime.now() - timedelta(days=1),
            completed_at=datetime.now() - timedelta(hours=12)
        )
        
        # Trabajo CANCELLED (cancelado)
        job7 = Job(
            client_id=client3.id,
            worker_id=None,
            title="Reparación de techo",
            description="Reparación de goteras en el techo.",
            service_type="Carpintería",
            status=JobStatus.CANCELLED,
            payment_method=PaymentMethod.CASH,
            base_fee=Decimal("250.00"),
            extras=Decimal("0.00"),
            total_amount=Decimal("250.00"),
            address="Av. Larco 123, Miraflores",
            latitude=Decimal("-12.1183"),
            longitude=Decimal("-77.0304")
        )
        
        db.add_all([job1, job_pending2, job_pending3, job_pending4, job_pending5, job_pending6, job2, job3, job4, job5, job6, job7])
        db.commit()
        db.refresh(job1)
        db.refresh(job_pending2)
        db.refresh(job_pending3)
        db.refresh(job_pending4)
        db.refresh(job_pending5)
        db.refresh(job_pending6)
        db.refresh(job2)
        db.refresh(job3)
        db.refresh(job4)
        db.refresh(job5)
        db.refresh(job6)
        db.refresh(job7)
        
        print(f"[OK] Trabajos creados: {db.query(Job).count()}")
        
        # ============================================
        # 3.5. CREAR APLICACIONES DE TRABAJADORES (Para chat)
        # ============================================
        print("[INFO] Creando aplicaciones de trabajadores...")
        
        # Para cada trabajo que tiene worker_id asignado, crear una JobApplication aceptada
        # Esto es necesario para que el chat funcione
        
        # job2 (ACCEPTED) - worker1
        application1 = JobApplication(
            job_id=job2.id,
            worker_id=worker1.id,
            is_accepted=True
        )
        
        # job3 (IN_ROUTE) - worker1
        application2 = JobApplication(
            job_id=job3.id,
            worker_id=worker1.id,
            is_accepted=True
        )
        
        # job4 (ON_SITE) - worker2
        application3 = JobApplication(
            job_id=job4.id,
            worker_id=worker2.id,
            is_accepted=True
        )
        
        # job5 (IN_PROGRESS) - worker2
        application4 = JobApplication(
            job_id=job5.id,
            worker_id=worker2.id,
            is_accepted=True
        )
        
        # job6 (COMPLETED) - worker1
        application5 = JobApplication(
            job_id=job6.id,
            worker_id=worker1.id,
            is_accepted=True
        )
        
        db.add_all([application1, application2, application3, application4, application5])
        db.commit()
        
        print(f"[OK] Aplicaciones creadas: {db.query(JobApplication).count()}")
        
        # ============================================
        # 4. CREAR COMISIONES
        # ============================================
        print("[INFO] Creando comisiones...")
        
        # Comisión PENDING (para trabajo completado)
        commission1 = Commission(
            worker_id=worker1.id,
            job_id=job6.id,
            amount=Decimal("60.00"),  # 10% de 600.00
            status=CommissionStatus.PENDING
        )
        
        # Comisión PAYMENT_SUBMITTED (trabajador envió código Yape)
        commission2 = Commission(
            worker_id=worker2.id,
            job_id=job5.id,
            amount=Decimal("35.00"),  # 10% de 350.00
            status=CommissionStatus.PAYMENT_SUBMITTED,
            payment_code="987654325",
            submitted_at=datetime.now() - timedelta(hours=1)
        )
        
        db.add_all([commission1, commission2])
        db.commit()
        
        print(f"[OK] Comisiones creadas: {db.query(Commission).count()}")
        
        # ============================================
        # 5. CREAR RATINGS
        # ============================================
        print("[INFO] Creando calificaciones...")
        
        rating1 = Rating(
            job_id=job6.id,
            client_rating=5,
            client_comment="Excelente trabajo, muy profesional y puntual. Lo recomiendo.",
            worker_rating=5,
            worker_comment="Cliente muy amable y claro en sus requerimientos."
        )
        
        db.add(rating1)
        db.commit()
        
        print(f"[OK] Ratings creados: {db.query(Rating).count()}")
        
        # ============================================
        # 6. CREAR MENSAJES (OPCIONAL)
        # ============================================
        print("[INFO] Creando mensajes de chat...")
        
        # Mensajes para job2 (ACCEPTED) - usar application1
        message1 = Message(
            job_id=job2.id,
            application_id=application1.id,
            sender_id=client2.id,
            content="Hola, ¿a qué hora llegarás?",
            created_at=datetime.now() - timedelta(hours=3)
        )
        
        message2 = Message(
            job_id=job2.id,
            application_id=application1.id,
            sender_id=worker_user1.id,
            content="Hola, llegaré alrededor de las 2 PM. ¿Te parece bien?",
            created_at=datetime.now() - timedelta(hours=2, minutes=30)
        )
        
        message3 = Message(
            job_id=job2.id,
            application_id=application1.id,
            sender_id=client2.id,
            content="Perfecto, te espero.",
            created_at=datetime.now() - timedelta(hours=2)
        )
        
        # Mensajes para job3 (IN_ROUTE) - usar application2
        message4 = Message(
            job_id=job3.id,
            application_id=application2.id,
            sender_id=worker_user1.id,
            content="Ya estoy en camino, llegaré en 15 minutos.",
            created_at=datetime.now() - timedelta(minutes=20)
        )
        
        message5 = Message(
            job_id=job3.id,
            application_id=application2.id,
            sender_id=client1.id,
            content="Perfecto, gracias.",
            created_at=datetime.now() - timedelta(minutes=15)
        )
        
        db.add_all([message1, message2, message3, message4, message5])
        db.commit()
        
        print(f"[OK] Mensajes creados: {db.query(Message).count()}")
        
        # ============================================
        # RESUMEN
        # ============================================
        print("\n" + "="*50)
        print("[OK] SEEDERS COMPLETADOS EXITOSAMENTE")
        print("="*50)
        print(f"Usuarios: {db.query(User).count()}")
        print(f"   - Clientes: {db.query(User).filter(User.role == UserRole.CLIENT).count()}")
        print(f"   - Trabajadores: {db.query(User).filter(User.role == UserRole.WORKER).count()}")
        print(f"   - Managers: {db.query(User).filter(User.role == UserRole.MANAGER).count()}")
        print(f"Trabajadores: {db.query(Worker).count()}")
        print(f"Suscripciones: {db.query(WorkerSubscription).count()}")
        print(f"Trabajos: {db.query(Job).count()}")
        print(f"Aplicaciones: {db.query(JobApplication).count()}")
        print(f"Comisiones: {db.query(Commission).count()}")
        print(f"Ratings: {db.query(Rating).count()}")
        print(f"Mensajes: {db.query(Message).count()}")
        print("\nCREDENCIALES DE PRUEBA:")
        print("   Cliente 1: cliente1@test.com / password123")
        print("   Cliente 2: cliente2@test.com / password123")
        print("   Cliente 3: cliente3@test.com / password123")
        print("   Trabajador 1: trabajador1@test.com / password123")
        print("   Trabajador 2: trabajador2@test.com / password123")
        print("   Trabajador 3: trabajador3@test.com / password123")
        print("   Manager: manager@test.com / password123")
        print("="*50)
        
    except Exception as e:
        print(f"[ERROR] Error al crear seeders: {str(e)}")
        import traceback
        traceback.print_exc()
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    seed_data()

